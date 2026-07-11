package com.sawhub.hub.mentorado;

import com.sawhub.hub.common.CsvUtils;
import com.sawhub.hub.common.dto.ImportErro;
import com.sawhub.hub.common.dto.ImportResultResponse;
import com.sawhub.hub.security.Usuario;
import com.sawhub.hub.security.UsuarioRepository;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** M22 — export/import CSV de {@link Mentorado}. Import é SÓ bulk-UPDATE de mentorados que já
 * existem (resolvidos por e-mail) — nunca cria conta/senha nova, ver Blueprint (ROADMAP.md).
 *
 * <p>Duas passadas de propósito, diferente do padrão de {@code LancamentoCsvService} (M21): lá
 * cada linha virava uma entidade NOVA (transiente, nunca tocava a sessão do Hibernate antes do
 * {@code saveAll()} final). Aqui é preciso CARREGAR o {@link Mentorado} existente pra validar a
 * linha — a partir do carregamento ele já é uma entidade GERENCIADA, e se ela for mutada antes de
 * saber se outra linha vai falhar, o {@code SELECT} de uma linha seguinte pode disparar um
 * auto-flush do Hibernate e mandar o UPDATE pro Postgres dentro da mesma transação — depois disso
 * nada desfaz. Por isso a primeira passada só LÊ (nunca chama {@code atualizar}/{@code ativar}/
 * {@code desativar}/{@code definirVencimentoPlano}), e só a segunda passada — que só roda se a
 * primeira não teve erro nenhum — muta e salva. */
@Service
public class MentoradoCsvService {

    private static final String[] CABECALHO = {"email", "nome", "negocio", "plano", "vencimentoPlano", "status"};
    private static final int LIMITE_LINHAS = 5000;

    private final MentoradoRepository mentoradoRepository;
    private final UsuarioRepository usuarioRepository;

    public MentoradoCsvService(MentoradoRepository mentoradoRepository, UsuarioRepository usuarioRepository) {
        this.mentoradoRepository = mentoradoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public String exportar(Plano plano, StatusMentorado status, String busca) {
        List<Mentorado> mentorados = mentoradoRepository.buscarComFiltro(plano, status, busca);
        StringWriter destino = new StringWriter();
        CSVFormat formato = CSVFormat.Builder.create().setDelimiter(';').setHeader(CABECALHO).build();
        try (CSVPrinter printer = new CSVPrinter(destino, formato)) {
            for (Mentorado m : mentorados) {
                printer.printRecord(
                        m.getUsuario().getEmail(),
                        CsvUtils.neutralizarFormula(m.getNome()),
                        CsvUtils.neutralizarFormula(m.getNegocio() == null ? "" : m.getNegocio()),
                        m.getPlano().name(),
                        m.getVencimentoPlano() == null ? "" : CsvUtils.formatarData(m.getVencimentoPlano()),
                        m.getStatus().name());
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

        // Primeira passada — só validação/leitura, nenhuma entidade é mutada aqui.
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

        // Segunda passada — só roda se TODAS as linhas passaram na primeira.
        List<Mentorado> paraSalvar = new ArrayList<>();
        for (LinhaValidada v : validas) {
            v.mentorado().atualizar(v.nome(), v.negocio(), v.plano());
            v.mentorado().definirVencimentoPlano(v.vencimentoPlano());
            if (v.status() == StatusMentorado.ATIVO) {
                v.mentorado().ativar();
            } else {
                v.mentorado().desativar();
            }
            paraSalvar.add(v.mentorado());
        }
        mentoradoRepository.saveAll(paraSalvar);
        return new ImportResultResponse(registros.size(), paraSalvar.size(), List.of());
    }

    private LinhaValidada validarLinha(CSVRecord registro) {
        String email = registro.get("email");
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("E-mail em branco.");
        }
        Usuario usuario = usuarioRepository.findByEmail(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Mentorado com e-mail \"" + email + "\" não encontrado."));
        Mentorado mentorado = mentoradoRepository.findByUsuario(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Mentorado com e-mail \"" + email + "\" não encontrado."));

        String nome = registro.get("nome");
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome em branco.");
        }
        String negocio = registro.get("negocio");
        Plano plano = parseEnum(Plano.class, registro.get("plano"), "Plano");
        LocalDate vencimentoPlano = parseDataOpcional(registro.get("vencimentoPlano"));
        StatusMentorado status = parseEnum(StatusMentorado.class, registro.get("status"), "Status");

        return new LinhaValidada(mentorado, nome, negocio == null || negocio.isBlank() ? null : negocio,
                plano, vencimentoPlano, status);
    }

    private record LinhaValidada(Mentorado mentorado, String nome, String negocio, Plano plano,
                                  LocalDate vencimentoPlano, StatusMentorado status) {
    }

    private static LocalDate parseDataOpcional(String bruto) {
        if (bruto == null || bruto.isBlank()) {
            return null;
        }
        return CsvUtils.parseData(bruto);
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> tipo, String bruto, String rotulo) {
        if (bruto == null || bruto.isBlank()) {
            throw new IllegalArgumentException(rotulo + " em branco.");
        }
        try {
            return Enum.valueOf(tipo, bruto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(rotulo + " \"" + bruto + "\" inválido.");
        }
    }

    private static void exigirColunas(List<String> cabecalhoEncontrado) {
        for (String esperada : CABECALHO) {
            if (!cabecalhoEncontrado.contains(esperada)) {
                throw new IllegalArgumentException(
                        "CSV sem a coluna \"" + esperada + "\". Colunas esperadas: " + String.join(", ", CABECALHO));
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
