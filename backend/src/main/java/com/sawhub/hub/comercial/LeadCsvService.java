package com.sawhub.hub.comercial;

import com.sawhub.hub.common.CsvUtils;
import com.sawhub.hub.common.dto.ImportErro;
import com.sawhub.hub.common.dto.ImportResultResponse;
import com.sawhub.hub.mentorado.Plano;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** M22 — export/import CSV de {@link Lead}. Import é sempre criação de lead novo em
 * {@link StatusLead#SOLICITACAO} — mesmo estágio do formulário público de captação (confirmado
 * com o Marcos, ver Blueprint no ROADMAP.md) — nunca anda a máquina de estado nem resolve
 * vendedor por nome. Entidades transientes (nunca carregadas do banco antes de salvar), mesmo
 * padrão de passada única do {@code LancamentoCsvService} (M21). */
@Service
public class LeadCsvService {

    private static final String[] CABECALHO_IMPORT = {"nome", "email", "telefone", "mensagem", "planoInteresse"};
    private static final String[] CABECALHO_EXPORT = {
            "nome", "email", "telefone", "mensagem", "planoInteresse",
            "status", "vendedor", "planoFechado", "motivoPerdido", "dataFechamento"
    };
    private static final int LIMITE_LINHAS = 5000;
    private static final Pattern EMAIL_SIMPLES = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final DateTimeFormatter DATA_HORA_PT_BR =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Sao_Paulo"));

    private final LeadService leadService;
    private final LeadRepository leadRepository;

    public LeadCsvService(LeadService leadService, LeadRepository leadRepository) {
        this.leadService = leadService;
        this.leadRepository = leadRepository;
    }

    public String exportar(StatusLead status, UUID vendedorId) {
        List<Lead> leads = leadService.listar(status, vendedorId);
        StringWriter destino = new StringWriter();
        CSVFormat formato = CSVFormat.Builder.create().setDelimiter(';').setHeader(CABECALHO_EXPORT).build();
        try (CSVPrinter printer = new CSVPrinter(destino, formato)) {
            for (Lead l : leads) {
                printer.printRecord(
                        CsvUtils.neutralizarFormula(l.getNome()),
                        l.getEmail(),
                        l.getTelefone() == null ? "" : l.getTelefone(),
                        CsvUtils.neutralizarFormula(l.getMensagem() == null ? "" : l.getMensagem()),
                        l.getPlanoInteresse() == null ? "" : l.getPlanoInteresse().name(),
                        l.getStatus().name(),
                        l.getVendedor() == null ? "" : CsvUtils.neutralizarFormula(l.getVendedor().getNome()),
                        l.getPlanoFechado() == null ? "" : l.getPlanoFechado().name(),
                        l.getMotivoPerdido() == null ? "" : CsvUtils.neutralizarFormula(l.getMotivoPerdido()),
                        l.getDataFechamento() == null ? "" : DATA_HORA_PT_BR.format(l.getDataFechamento()));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gerar o CSV.", e);
        }
        return destino.toString();
    }

    @Transactional
    public ImportResultResponse importar(MultipartFile arquivo) {
        CsvUtils.exigirArquivoCsv(arquivo);
        String conteudo = lerConteudo(arquivo);
        String primeiraLinha = conteudo.lines().findFirst().orElse("");
        CSVFormat formato = CSVFormat.Builder.create()
                .setDelimiter(CsvUtils.detectarDelimitador(primeiraLinha))
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .build();

        List<CSVRecord> registros;
        try (CSVParser parser = CSVParser.parse(conteudo, formato)) {
            exigirColunas(parser.getHeaderNames());
            registros = parser.getRecords();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível interpretar o CSV.", e);
        }

        if (registros.isEmpty()) {
            throw new IllegalArgumentException("Arquivo CSV sem nenhuma linha de dados.");
        }
        if (registros.size() > LIMITE_LINHAS) {
            throw new IllegalArgumentException("Arquivo excede o limite de " + LIMITE_LINHAS + " linhas.");
        }

        List<ImportErro> erros = new ArrayList<>();
        List<Lead> paraSalvar = new ArrayList<>();
        for (CSVRecord registro : registros) {
            int linha = (int) registro.getRecordNumber() + 1;
            try {
                paraSalvar.add(parseLinha(registro));
            } catch (IllegalArgumentException e) {
                erros.add(new ImportErro(linha, e.getMessage()));
            }
        }

        if (!erros.isEmpty()) {
            return new ImportResultResponse(registros.size(), 0, erros);
        }
        leadRepository.saveAll(paraSalvar);
        return new ImportResultResponse(registros.size(), paraSalvar.size(), List.of());
    }

    // Mesmos limites de CriarLeadRequest (H1.3): evitam um VARCHAR estourado virar um 409/500
    // genérico do GlobalExceptionHandler em vez de um erro de linha claro — igualmente relevante
    // aqui, já que o import bypassa o @Valid do endpoint público.
    private Lead parseLinha(CSVRecord registro) {
        String nome = registro.get("nome");
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome em branco.");
        }
        if (nome.length() > 120) {
            throw new IllegalArgumentException("Nome excede 120 caracteres.");
        }
        String email = registro.get("email");
        if (email == null || email.isBlank() || !EMAIL_SIMPLES.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("E-mail \"" + email + "\" inválido.");
        }
        if (email.length() > 255) {
            throw new IllegalArgumentException("E-mail excede 255 caracteres.");
        }
        String telefone = registro.get("telefone");
        if (telefone != null && telefone.length() > 20) {
            throw new IllegalArgumentException("Telefone excede 20 caracteres.");
        }
        String mensagem = registro.get("mensagem");
        if (mensagem != null && mensagem.length() > 500) {
            throw new IllegalArgumentException("Mensagem excede 500 caracteres.");
        }
        Plano planoInteresse = parsePlanoOpcional(registro.get("planoInteresse"));

        return new Lead(nome.trim(), email.trim(), telefone == null || telefone.isBlank() ? null : telefone,
                mensagem == null || mensagem.isBlank() ? null : mensagem, planoInteresse);
    }

    private static Plano parsePlanoOpcional(String bruto) {
        if (bruto == null || bruto.isBlank()) {
            return null;
        }
        try {
            return Plano.valueOf(bruto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Plano de interesse \"" + bruto + "\" inválido.");
        }
    }

    private static void exigirColunas(List<String> cabecalhoEncontrado) {
        for (String esperada : CABECALHO_IMPORT) {
            if (!cabecalhoEncontrado.contains(esperada)) {
                throw new IllegalArgumentException(
                        "CSV sem a coluna \"" + esperada + "\". Colunas esperadas: " + String.join(", ", CABECALHO_IMPORT));
            }
        }
    }

    private static String lerConteudo(MultipartFile arquivo) {
        try {
            return new String(arquivo.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler o arquivo.", e);
        }
    }
}
