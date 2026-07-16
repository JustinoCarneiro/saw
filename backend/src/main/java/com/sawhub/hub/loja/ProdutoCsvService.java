package com.sawhub.hub.loja;

import com.sawhub.hub.common.CsvUtils;
import com.sawhub.hub.common.dto.ImportErro;
import com.sawhub.hub.common.dto.ImportResultResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** M23 — export/import CSV de {@link Produto}. Import cria produtos novos como rascunho
 * ({@code publicado=false}). */
@Service
public class ProdutoCsvService {

    private static final String[] CABECALHO_EXPORT = {
            "titulo", "descricao", "categoria", "preco", "precoOriginal", "avaliacaoMedia",
            "destaque", "arquivoUrl", "imagemUrl", "vendaEmAtacado", "publicado", "vendas"
    };
    private static final String[] CABECALHO_IMPORT = {
            "titulo", "descricao", "categoria", "preco", "precoOriginal", "avaliacaoMedia",
            "destaque", "arquivoUrl", "imagemUrl", "vendaEmAtacado"
    };
    private static final int LIMITE_LINHAS = 5000;

    private final ProdutoRepository produtoRepository;

    public ProdutoCsvService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    public String exportar(CategoriaProduto categoria, Boolean publicado, Boolean destaque, String busca) {
        List<Produto> produtos = produtoRepository.findAll().stream()
                .filter(p -> categoria == null || p.getCategoria() == categoria)
                .filter(p -> publicado == null || p.isPublicado() == publicado)
                .filter(p -> destaque == null || p.isDestaque() == destaque)
                .filter(p -> busca == null || busca.isBlank()
                        || p.getTitulo().toLowerCase().contains(busca.toLowerCase()))
                .toList();
        StringWriter destino = new StringWriter();
        CSVFormat formato = CSVFormat.Builder.create().setDelimiter(';').setHeader(CABECALHO_EXPORT).build();
        try (CSVPrinter printer = new CSVPrinter(destino, formato)) {
            for (Produto p : produtos) {
                printer.printRecord(
                        CsvUtils.neutralizarFormula(p.getTitulo()),
                        CsvUtils.neutralizarFormula(p.getDescricao()),
                        p.getCategoria().name(),
                        p.getPreco().toPlainString().replace('.', ','),
                        p.getPrecoOriginal() == null ? "" : p.getPrecoOriginal().toPlainString().replace('.', ','),
                        p.getAvaliacaoMedia() == null ? "" : p.getAvaliacaoMedia().toPlainString().replace('.', ','),
                        p.isDestaque(),
                        p.getArquivoUrl(),
                        p.getImagemUrl() == null ? "" : p.getImagemUrl(),
                        p.isVendaEmAtacado(),
                        p.isPublicado(),
                        p.getVendas());
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

        List<ImportErro> erros = new ArrayList<>();
        List<Produto> paraSalvar = new ArrayList<>();
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
        produtoRepository.saveAll(paraSalvar);
        return new ImportResultResponse(registros.size(), paraSalvar.size(), List.of());
    }

    private Produto parseLinha(CSVRecord registro, char delimitador) {
        String titulo = registro.get("titulo");
        CsvUtils.exigirNaoVazio(titulo, "Título");
        CsvUtils.exigirTamanhoMaximo(titulo, 255, "Título");

        String descricao = registro.get("descricao");
        CsvUtils.exigirNaoVazio(descricao, "Descrição");

        CategoriaProduto categoria = CsvUtils.parseEnum(CategoriaProduto.class, registro.get("categoria"), "Categoria");

        BigDecimal preco = CsvUtils.parseValor(registro.get("preco"), delimitador);
        if (preco.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Preço deve ser maior que zero.");
        }

        BigDecimal precoOriginal = parseValorOpcional(registro.get("precoOriginal"), delimitador, "Preço original");
        BigDecimal avaliacaoMedia = parseAvaliacaoOpcional(registro.get("avaliacaoMedia"), delimitador);

        boolean destaque = CsvUtils.parseBooleano(registro.get("destaque"), "Destaque");

        String arquivoUrl = registro.get("arquivoUrl");
        CsvUtils.exigirUrl(arquivoUrl, "URL do arquivo");

        String imagemUrl = registro.get("imagemUrl");
        CsvUtils.exigirUrlOpcional(imagemUrl, "URL da imagem");

        boolean vendaEmAtacado = CsvUtils.parseBooleano(registro.get("vendaEmAtacado"), "Venda em atacado");

        return new Produto(titulo.trim(), descricao.trim(), categoria, preco,
                precoOriginal, avaliacaoMedia, destaque, arquivoUrl.trim(),
                imagemUrl == null || imagemUrl.isBlank() ? null : imagemUrl.trim(), vendaEmAtacado);
    }

    private static BigDecimal parseValorOpcional(String bruto, char delimitador, String rotulo) {
        if (bruto == null || bruto.isBlank()) {
            return null;
        }
        BigDecimal valor = CsvUtils.parseValor(bruto, delimitador);
        if (valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(rotulo + " não pode ser negativo.");
        }
        return valor;
    }

    private static BigDecimal parseAvaliacaoOpcional(String bruto, char delimitador) {
        if (bruto == null || bruto.isBlank()) {
            return null;
        }
        BigDecimal valor = CsvUtils.parseValor(bruto, delimitador);
        if (valor.compareTo(BigDecimal.ZERO) < 0 || valor.compareTo(new BigDecimal("5.0")) > 0) {
            throw new IllegalArgumentException("Avaliação \"" + bruto + "\" inválida (use 0.0 a 5.0).");
        }
        return valor;
    }
}
