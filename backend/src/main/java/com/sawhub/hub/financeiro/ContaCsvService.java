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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** M21 — export/import CSV de {@link ContaPagarReceber}. Mesmo import tudo-ou-nada de
 * {@link LancamentoCsvService} — ver justificativa no Blueprint (ROADMAP.md). */
@Service
public class ContaCsvService {

    private static final String[] CABECALHO = {"tipo", "descricao", "valor", "dataVencimento", "categoria"};
    private static final int LIMITE_LINHAS = 5000;

    private final ContaPagarReceberService contaService;
    private final ContaPagarReceberRepository contaRepository;
    private final CategoriaFinanceiraRepository categoriaRepository;

    public ContaCsvService(ContaPagarReceberService contaService, ContaPagarReceberRepository contaRepository,
                            CategoriaFinanceiraRepository categoriaRepository) {
        this.contaService = contaService;
        this.contaRepository = contaRepository;
        this.categoriaRepository = categoriaRepository;
    }

    public String exportar(TipoConta tipo, StatusConta status, Integer ano, Integer mes) {
        List<ContaPagarReceber> contas = contaService.listar(tipo, status, ano, mes);
        StringWriter destino = new StringWriter();
        CSVFormat formato = CSVFormat.Builder.create().setDelimiter(';').setHeader(CABECALHO).build();
        try (CSVPrinter printer = new CSVPrinter(destino, formato)) {
            for (ContaPagarReceber c : contas) {
                printer.printRecord(
                        c.getTipo().name(),
                        CsvUtils.neutralizarFormula(c.getDescricao()),
                        c.getValor().toPlainString().replace('.', ','),
                        CsvUtils.formatarData(c.getDataVencimento()),
                        c.getCategoria() == null ? "" : CsvUtils.neutralizarFormula(c.getCategoria().getNome()));
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
        List<ContaPagarReceber> paraSalvar = new ArrayList<>();
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
        contaRepository.saveAll(paraSalvar);
        return new ImportResultResponse(registros.size(), paraSalvar.size(), List.of());
    }

    private ContaPagarReceber parseLinha(CSVRecord registro, char delimitador) {
        TipoConta tipo = parseEnum(TipoConta.class, registro.get("tipo"), "Tipo");
        String descricao = registro.get("descricao");
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("Descrição em branco.");
        }
        BigDecimal valor = CsvUtils.parseValor(registro.get("valor"), delimitador);
        if (valor.compareTo(BigDecimal.valueOf(0.01)) < 0) {
            throw new IllegalArgumentException("Valor deve ser maior ou igual a 0,01.");
        }
        LocalDate dataVencimento = CsvUtils.parseData(registro.get("dataVencimento"));
        CategoriaFinanceira categoria = resolverCategoriaOpcional(registro.get("categoria"), tipo);

        return new ContaPagarReceber(tipo, descricao, valor, dataVencimento, categoria);
    }

    private CategoriaFinanceira resolverCategoriaOpcional(String nome, TipoConta tipoConta) {
        if (nome == null || nome.isBlank()) {
            return null;
        }
        List<CategoriaFinanceira> candidatas = categoriaRepository.findByNomeIgnoreCase(nome.trim());
        if (candidatas.isEmpty()) {
            throw new IllegalArgumentException("Categoria \"" + nome + "\" não encontrada.");
        }
        if (candidatas.size() > 1) {
            throw new IllegalArgumentException("Categoria \"" + nome + "\" é ambígua (existe mais de uma com esse nome).");
        }
        CategoriaFinanceira categoria = candidatas.get(0);
        TipoLancamento tipoLancamentoEsperado = tipoConta == TipoConta.A_PAGAR ? TipoLancamento.DESPESA : TipoLancamento.RECEITA;
        if (categoria.getTipo() != tipoLancamentoEsperado) {
            throw new IllegalArgumentException("Categoria \"" + nome + "\" é do tipo " + categoria.getTipo()
                    + ", incompatível com uma conta " + tipoConta + ".");
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
