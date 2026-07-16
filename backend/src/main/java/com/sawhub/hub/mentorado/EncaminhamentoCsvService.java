package com.sawhub.hub.mentorado;

import com.sawhub.hub.common.CsvUtils;
import com.sawhub.hub.common.dto.ImportErro;
import com.sawhub.hub.common.dto.ImportResultResponse;
import com.sawhub.hub.security.UsuarioRepository;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** M23 — export/import CSV de {@link Encaminhamento}. Import cria tarefas em nome de mentorados
 * (via e-mail). Peso sempre 1 (peso 2 é exclusivo do fluxo Admin/ata, M06). Sem vínculo a Meta.
 * Duas passadas (mesma justificativa do M22). */
@Service
public class EncaminhamentoCsvService {

    private static final String[] CABECALHO_EXPORT = {
            "emailMentorado", "nomeMentorado", "titulo", "peso", "status", "prazo", "prioridade"
    };
    private static final String[] CABECALHO_IMPORT = {"emailMentorado", "titulo", "prazo", "prioridade"};
    private static final int LIMITE_LINHAS = 5000;

    private final EncaminhamentoRepository encaminhamentoRepository;
    private final MentoradoRepository mentoradoRepository;
    private final UsuarioRepository usuarioRepository;

    public EncaminhamentoCsvService(EncaminhamentoRepository encaminhamentoRepository,
                                     MentoradoRepository mentoradoRepository,
                                     UsuarioRepository usuarioRepository) {
        this.encaminhamentoRepository = encaminhamentoRepository;
        this.mentoradoRepository = mentoradoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public String exportar() {
        List<Encaminhamento> encaminhamentos = encaminhamentoRepository.findAll();
        StringWriter destino = new StringWriter();
        CSVFormat formato = CSVFormat.Builder.create().setDelimiter(';').setHeader(CABECALHO_EXPORT).build();
        try (CSVPrinter printer = new CSVPrinter(destino, formato)) {
            for (Encaminhamento e : encaminhamentos) {
                printer.printRecord(
                        e.getMentorado().getUsuario().getEmail(),
                        CsvUtils.neutralizarFormula(e.getMentorado().getNome()),
                        CsvUtils.neutralizarFormula(e.getTitulo()),
                        e.getPeso(),
                        e.getStatus().name(),
                        e.getPrazo() == null ? "" : CsvUtils.formatarData(e.getPrazo()),
                        e.getPrioridade().name());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gerar o CSV.", e);
        }
        return destino.toString();
    }

    @Transactional
    public ImportResultResponse importar(MultipartFile arquivo) {
        CsvUtils.exigirArquivoCsv(arquivo);
        String conteudo = CsvUtils.lerConteudo(arquivo);
        String primeiraLinha = conteudo.lines().findFirst().orElse("");
        char delimitador = CsvUtils.detectarDelimitador(primeiraLinha);

        CSVFormat formato = CSVFormat.Builder.create()
                .setDelimiter(delimitador)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .build();

        List<CSVRecord> registros;
        try (CSVParser parser = CSVParser.parse(conteudo, formato)) {
            CsvUtils.exigirColunas(parser.getHeaderNames(), CABECALHO_IMPORT);
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

        // Primeira passada — só validação/leitura.
        List<ImportErro> erros = new ArrayList<>();
        List<LinhaValidada> validas = new ArrayList<>();
        for (CSVRecord registro : registros) {
            int linha = (int) registro.getRecordNumber() + 1;
            try {
                validas.add(validarLinha(registro));
            } catch (IllegalArgumentException e) {
                erros.add(new ImportErro(linha, e.getMessage()));
            }
        }

        if (!erros.isEmpty()) {
            return new ImportResultResponse(registros.size(), 0, erros);
        }

        // Segunda passada — cria os encaminhamentos.
        List<Encaminhamento> paraSalvar = new ArrayList<>();
        for (LinhaValidada v : validas) {
            paraSalvar.add(new Encaminhamento(v.mentorado(), v.titulo(), v.prazo(), v.prioridade(), null));
        }
        encaminhamentoRepository.saveAll(paraSalvar);
        return new ImportResultResponse(registros.size(), paraSalvar.size(), List.of());
    }

    private LinhaValidada validarLinha(CSVRecord registro) {
        String email = CsvUtils.normalizarEmail(registro.get("emailMentorado"));
        Mentorado mentorado = usuarioRepository.findByEmail(email)
                .flatMap(mentoradoRepository::findByUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Mentorado com e-mail \"" + email + "\" não encontrado."));

        String titulo = registro.get("titulo");
        CsvUtils.exigirNaoVazio(titulo, "Título");

        LocalDate prazo = CsvUtils.parseDataOpcional(registro.get("prazo"));
        Prioridade prioridade = CsvUtils.parseEnumOpcional(Prioridade.class, registro.get("prioridade"), "Prioridade", Prioridade.MEDIA);

        return new LinhaValidada(mentorado, titulo.trim(), prazo, prioridade);
    }

    private record LinhaValidada(Mentorado mentorado, String titulo, LocalDate prazo, Prioridade prioridade) {
    }
}
