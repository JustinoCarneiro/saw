package com.sawhub.hub.mentorado;

import com.sawhub.hub.common.CsvUtils;
import com.sawhub.hub.common.dto.ImportErro;
import com.sawhub.hub.mentorado.dto.ImportMentoradoDiretoResultResponse;
import com.sawhub.hub.mentorado.dto.ImportarMentoradoDiretoLinha;
import com.sawhub.hub.mentorado.dto.MentoradoCriadoResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** M23 item 4 (bulk-CREATE, 19/07/2026), estendido no M28 (change request, 21/07/2026, "import
 * único") — import CSV que CRIA Mentorado (+ Usuario com senha temporária, + Lead já FECHADO) OU,
 * se o e-mail já é de um mentorado cadastrado, ATUALIZA os campos desse mentorado. Antes do M28
 * havia dois botões de import na tela (este só criava, {@link MentoradoCsvService} só atualizava
 * por e-mail com um subconjunto de 6 campos) — o cliente achou confuso ter os dois, e um import só
 * criar-ou-atualizar cobre os dois casos com o mesmo conjunto completo de campos. Pensado
 * originalmente pra migrar de uma vez as ~40 empresas reais que hoje só existem no Notion ("CRM
 * Saw") — ver docs/reuniao-2026-07-17-atualizacoes.md.
 *
 * <p>Duas passadas, mesmo padrão de {@code TeamCsvService}: primeira só valida/resolve (nenhuma
 * entidade é criada OU mutada aqui — só decide, por e-mail, se a linha vai criar ou atualizar),
 * segunda executa de verdade — só roda se TODAS as linhas passaram na primeira. */
@Service
public class MentoradoDiretoCsvService {

    // Ordem alinhada ao contrato do Blueprint M24 (ROADMAP.md) — o parser Notion→CSV (a escrever
    // quando o export completo do Notion chegar, ver docs/reuniao-2026-07-17-atualizacoes.md)
    // mira nesta ordem como formato de saída.
    private static final String[] CABECALHO = {
            "email", "nome", "negocio", "nomeFantasia", "cnpj", "socios", "telefone", "tipoContrato",
            "valorContrato", "dataFechamentoContrato", "faturamentoAnual", "quantidadeColaboradores",
            "empresaRegularizada", "quantidadeLojas", "cmvDefinido", "cmvDetalhe", "tempoMedioAtendimento",
            "culturaConstruida", "processosDesenhados"
    };
    private static final int LIMITE_LINHAS = 5000;
    // Mesmo critério de AtualizarDadosContratoRequest.cnpj — aceita formatado ou 14 dígitos puros.
    private static final Pattern CNPJ_REGEX = Pattern.compile("^\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}$|^\\d{14}$");

    private final MentoradoAdminService mentoradoAdminService;

    public MentoradoDiretoCsvService(MentoradoAdminService mentoradoAdminService) {
        this.mentoradoAdminService = mentoradoAdminService;
    }

    @Transactional
    public ImportMentoradoDiretoResultResponse importar(MultipartFile arquivo) {
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
            CsvUtils.exigirColunas(parser.getHeaderNames(), CABECALHO);
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

        // Primeira passada — só validação/leitura, nenhuma entidade é criada aqui.
        List<ImportErro> erros = new ArrayList<>();
        List<ImportarMentoradoDiretoLinha> validas = new ArrayList<>();
        Set<String> emailsNoArquivo = new HashSet<>();
        for (CSVRecord registro : registros) {
            int linha = (int) registro.getRecordNumber() + 1;
            try {
                ImportarMentoradoDiretoLinha v = validarLinha(registro, delimitador, emailsNoArquivo);
                validas.add(v);
                emailsNoArquivo.add(v.email());
            } catch (IllegalArgumentException e) {
                erros.add(new ImportErro(linha, e.getMessage()));
            }
        }

        if (!erros.isEmpty()) {
            return new ImportMentoradoDiretoResultResponse(registros.size(), 0, 0, erros, List.of());
        }

        // Segunda passada — só roda se TODAS as linhas passaram na primeira. mentoradoExistenteId
        // não-nulo (M28 — import único) decide entre atualizar um mentorado já cadastrado ou criar
        // um novo, linha a linha.
        List<MentoradoCriadoResponse> criados = new ArrayList<>();
        int atualizados = 0;
        for (ImportarMentoradoDiretoLinha v : validas) {
            if (v.mentoradoExistenteId() != null) {
                mentoradoAdminService.atualizarDeImportacao(v.mentoradoExistenteId(), v);
                atualizados++;
            } else {
                var resultado = mentoradoAdminService.criarDiretoDeImportacao(v);
                criados.add(MentoradoCriadoResponse.from(resultado.mentorado(), resultado.senhaTemporaria()));
            }
        }
        return new ImportMentoradoDiretoResultResponse(registros.size(), criados.size(), atualizados, List.of(), criados);
    }

    private ImportarMentoradoDiretoLinha validarLinha(CSVRecord registro, char delimitador,
                                                        Set<String> emailsJaVistosNoArquivo) {
        String email = CsvUtils.normalizarEmail(registro.get("email"));
        if (!email.contains("@")) {
            throw new IllegalArgumentException("E-mail \"" + email + "\" inválido.");
        }
        CsvUtils.exigirTamanhoMaximo(email, 255, "E-mail");
        if (emailsJaVistosNoArquivo.contains(email)) {
            throw new IllegalArgumentException("E-mail \"" + email + "\" duplicado no arquivo.");
        }
        // M28 — import único: e-mail já cadastrado como Mentorado vira ATUALIZAÇÃO, não erro. Só
        // continua sendo erro quando o e-mail já existe mas pertence a outro tipo de conta
        // (Colaborador/Admin) — aí não há Mentorado nenhum pra atualizar.
        Mentorado existente = mentoradoAdminService.buscarPorEmail(email);
        UUID mentoradoExistenteId = null;
        if (existente != null) {
            mentoradoExistenteId = existente.getId();
        } else if (mentoradoAdminService.existeContaComEmail(email)) {
            throw new IllegalArgumentException("E-mail \"" + email + "\" já existe mas não é um mentorado.");
        }

        String nome = registro.get("nome");
        CsvUtils.exigirNaoVazio(nome, "Nome");
        CsvUtils.exigirTamanhoMaximo(nome, 255, "Nome");

        String negocio = opcional(registro.get("negocio"));
        CsvUtils.exigirTamanhoMaximo(negocio, 255, "Negócio");
        String telefone = opcional(registro.get("telefone"));
        CsvUtils.exigirTamanhoMaximo(telefone, 30, "Telefone");

        TipoContrato tipoContrato = CsvUtils.parseEnum(TipoContrato.class, registro.get("tipoContrato"), "Tipo de contrato");
        BigDecimal valorContrato = parseValorOpcional(registro.get("valorContrato"), delimitador, "Valor do contrato");
        LocalDate dataFechamentoContrato = CsvUtils.parseDataOpcional(registro.get("dataFechamentoContrato"));

        String nomeFantasia = opcional(registro.get("nomeFantasia"));
        CsvUtils.exigirTamanhoMaximo(nomeFantasia, 255, "Nome fantasia");
        String cnpj = opcional(registro.get("cnpj"));
        if (cnpj != null && !CNPJ_REGEX.matcher(cnpj).matches()) {
            throw new IllegalArgumentException("CNPJ \"" + cnpj + "\" inválido (use 00.000.000/0000-00 ou 14 dígitos).");
        }
        String socios = opcional(registro.get("socios"));
        CsvUtils.exigirTamanhoMaximo(socios, 500, "Sócios");

        BigDecimal faturamentoAnual = parseValorOpcional(registro.get("faturamentoAnual"), delimitador, "Faturamento anual");
        Integer quantidadeColaboradores = CsvUtils.parseIntOpcional(registro.get("quantidadeColaboradores"), "Quantidade de colaboradores");
        Boolean empresaRegularizada = parseBooleanoOpcional(registro.get("empresaRegularizada"), "Empresa regularizada");
        Integer quantidadeLojas = CsvUtils.parseIntOpcional(registro.get("quantidadeLojas"), "Quantidade de lojas");
        RespostaSimNao cmvDefinido = CsvUtils.parseEnumOpcional(RespostaSimNao.class, registro.get("cmvDefinido"), "CMV definido", null);
        String cmvDetalhe = opcional(registro.get("cmvDetalhe"));
        CsvUtils.exigirTamanhoMaximo(cmvDetalhe, 255, "CMV detalhe");
        String tempoMedioAtendimento = opcional(registro.get("tempoMedioAtendimento"));
        CsvUtils.exigirTamanhoMaximo(tempoMedioAtendimento, 100, "Tempo médio de atendimento");
        EstadoImplementacao culturaConstruida =
                CsvUtils.parseEnumOpcional(EstadoImplementacao.class, registro.get("culturaConstruida"), "Cultura construída", null);
        EstadoImplementacao processosDesenhados =
                CsvUtils.parseEnumOpcional(EstadoImplementacao.class, registro.get("processosDesenhados"), "Processos desenhados", null);

        return new ImportarMentoradoDiretoLinha(email, nome.trim(), negocio, telefone, tipoContrato, valorContrato,
                dataFechamentoContrato, nomeFantasia, cnpj, socios, faturamentoAnual, quantidadeColaboradores,
                empresaRegularizada, quantidadeLojas, cmvDefinido, cmvDetalhe, tempoMedioAtendimento,
                culturaConstruida, processosDesenhados, mentoradoExistenteId);
    }

    private static String opcional(String bruto) {
        return bruto == null || bruto.isBlank() ? null : bruto.trim();
    }

    private static BigDecimal parseValorOpcional(String bruto, char delimitador, String rotulo) {
        if (bruto == null || bruto.isBlank()) {
            return null;
        }
        BigDecimal valor = CsvUtils.parseValor(bruto, delimitador);
        if (valor.signum() < 0) {
            throw new IllegalArgumentException(rotulo + " não pode ser negativo.");
        }
        return valor;
    }

    /** Aceita true/false/sim/nao/1/0 (case-insensitive); branco = não informado ({@code null}),
     * distinto de "não" explícito — diferente de {@link CsvUtils#parseBooleano}, que trata branco
     * como {@code false} (correto pros outros módulos, errado aqui: "empresa regularizada" sem
     * resposta não é o mesmo que "não regularizada"). */
    private static Boolean parseBooleanoOpcional(String bruto, String rotulo) {
        if (bruto == null || bruto.isBlank()) {
            return null;
        }
        return CsvUtils.parseBooleano(bruto, rotulo);
    }
}
