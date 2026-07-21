package com.sawhub.hub.financeiro;

import com.sawhub.hub.common.CsvUtils;
import com.sawhub.hub.common.dto.ImportErro;
import com.sawhub.hub.common.dto.ImportResultResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** M21 — export/import CSV de {@link LancamentoFinanceiro}. M26 fundiu {@code ContaCsvService}
 * aqui (mesma entidade agora, ver ROADMAP.md § "Blueprint (M26)"): ganhou {@code dataVencimento}
 * opcional no cabeçalho e um segundo método de export por vencimento (pra `/contas/export`) — o
 * import continua único (uma linha vira um lançamento, com ou sem vencimento). Import é
 * tudo-ou-nada: a entidade é imutável por linha, sem endpoint de exclusão pra desfazer um import
 * parcial ruim. */
@Service
public class LancamentoCsvService {

    private static final String[] CABECALHO = {
            "tipo", "categoria", "descricao", "valor", "dataCompetencia", "dataVencimento", "status"
    };
    private static final int LIMITE_LINHAS = 5000;

    private final LancamentoService lancamentoService;
    private final LancamentoFinanceiroRepository lancamentoRepository;
    private final CategoriaFinanceiraRepository categoriaRepository;

    public LancamentoCsvService(LancamentoService lancamentoService, LancamentoFinanceiroRepository lancamentoRepository,
                                 CategoriaFinanceiraRepository categoriaRepository) {
        this.lancamentoService = lancamentoService;
        this.lancamentoRepository = lancamentoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    public String exportarPorCompetencia(LocalDate de, LocalDate ate, TipoLancamento tipo, UUID categoriaId) {
        return exportar(lancamentoService.listar(de, ate, tipo, categoriaId));
    }

    /** M26 (absorvido de {@code ContaCsvService.exportar}) — mesmos filtros de
     * `GET /admin/financeiro/contas`. */
    public String exportarPorVencimento(TipoLancamento tipo, StatusLancamento status, Integer ano, Integer mes,
                                         UUID eventoId) {
        return exportar(lancamentoService.listarPorVencimento(tipo, status, ano, mes, eventoId));
    }

    private String exportar(List<LancamentoFinanceiro> lancamentos) {
        StringWriter destino = new StringWriter();
        CSVFormat formato = CSVFormat.Builder.create().setDelimiter(';').setHeader(CABECALHO).build();
        try (CSVPrinter printer = new CSVPrinter(destino, formato)) {
            for (LancamentoFinanceiro l : lancamentos) {
                printer.printRecord(
                        l.getTipo().name(),
                        CsvUtils.neutralizarFormula(l.getCategoria().getNome()),
                        CsvUtils.neutralizarFormula(l.getDescricao()),
                        l.getValor().toPlainString().replace('.', ','),
                        CsvUtils.formatarData(l.getDataCompetencia()),
                        l.getDataVencimento() == null ? "" : CsvUtils.formatarData(l.getDataVencimento()),
                        l.getStatus().name());
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

        List<ImportErro> erros = new ArrayList<>();
        List<LancamentoFinanceiro> paraSalvar = new ArrayList<>();
        for (CSVRecord registro : registros) {
            int linha = (int) registro.getRecordNumber() + 1;
            try {
                paraSalvar.add(parseLinha(registro, delimitador));
            } catch (IllegalArgumentException e) {
                erros.add(new ImportErro(linha, e.getMessage()));
            }
        }

        if (!erros.isEmpty()) {
            return new ImportResultResponse(registros.size(), 0, erros);
        }
        lancamentoRepository.saveAll(paraSalvar);
        return new ImportResultResponse(registros.size(), paraSalvar.size(), List.of());
    }

    private LancamentoFinanceiro parseLinha(CSVRecord registro, char delimitador) {
        TipoLancamento tipo = parseEnum(TipoLancamento.class, registro.get("tipo"), "Tipo");
        String nomeCategoria = registro.get("categoria");
        CategoriaFinanceira categoria = resolverCategoria(nomeCategoria, tipo);
        String descricao = registro.get("descricao");
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("Descrição em branco.");
        }
        BigDecimal valor = CsvUtils.parseValor(registro.get("valor"), delimitador);
        if (valor.compareTo(BigDecimal.valueOf(0.01)) < 0) {
            throw new IllegalArgumentException("Valor deve ser maior ou igual a 0,01.");
        }
        LocalDate dataCompetencia = CsvUtils.parseData(registro.get("dataCompetencia"));
        LocalDate dataVencimento = CsvUtils.parseDataOpcional(registro.get("dataVencimento"));
        StatusLancamento status = parseEnum(StatusLancamento.class, registro.get("status"), "Status");

        return new LancamentoFinanceiro(tipo, categoria, descricao, valor, dataCompetencia, status,
                null, dataVencimento);
    }

    private CategoriaFinanceira resolverCategoria(String nome, TipoLancamento tipo) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Categoria em branco.");
        }
        List<CategoriaFinanceira> candidatas = categoriaRepository.findByNomeIgnoreCase(nome.trim());
        if (candidatas.isEmpty()) {
            throw new IllegalArgumentException("Categoria \"" + nome + "\" não encontrada.");
        }
        if (candidatas.size() > 1) {
            throw new IllegalArgumentException("Categoria \"" + nome + "\" é ambígua (existe mais de uma com esse nome).");
        }
        CategoriaFinanceira categoria = candidatas.get(0);
        // Achado do Blueprint (M21): o formulário manual de criação limita o <select> de categoria
        // pelo tipo, mas o endpoint POST /lancamentos em si nunca validou essa consistência — o
        // import é uma superfície nova sem esse guarda-corpo de UI, então valida aqui.
        if (categoria.getTipo() != tipo) {
            throw new IllegalArgumentException("Categoria \"" + nome + "\" é do tipo " + categoria.getTipo()
                    + ", incompatível com o tipo " + tipo + " da linha.");
        }
        return categoria;
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
