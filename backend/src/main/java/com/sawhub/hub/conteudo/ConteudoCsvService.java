package com.sawhub.hub.conteudo;

import com.sawhub.hub.common.CsvUtils;
import com.sawhub.hub.common.dto.ImportErro;
import com.sawhub.hub.common.dto.ImportResultResponse;
import com.sawhub.hub.mentorado.Plano;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** M23 — export/import CSV de {@link Conteudo}. Import cria conteúdos como rascunho. */
@Service
public class ConteudoCsvService {

    private static final String[] CABECALHO_EXPORT = {"titulo", "tipo", "url", "planoMinimo", "duracaoMinutos", "publicado"};
    private static final String[] CABECALHO_IMPORT = {"titulo", "tipo", "url", "planoMinimo", "duracaoMinutos"};
    private static final int LIMITE_LINHAS = 5000;

    private final ConteudoRepository conteudoRepository;

    public ConteudoCsvService(ConteudoRepository conteudoRepository) {
        this.conteudoRepository = conteudoRepository;
    }

    public String exportar(TipoConteudo tipo, Plano planoMinimo, Boolean publicado) {
        List<Conteudo> conteudos = conteudoRepository.findAll().stream()
                .filter(c -> tipo == null || c.getTipo() == tipo)
                .filter(c -> planoMinimo == null || c.getPlanoMinimo() == planoMinimo)
                .filter(c -> publicado == null || c.isPublicado() == publicado)
                .toList();
        StringWriter destino = new StringWriter();
        CSVFormat formato = CSVFormat.Builder.create().setDelimiter(';').setHeader(CABECALHO_EXPORT).build();
        try (CSVPrinter printer = new CSVPrinter(destino, formato)) {
            for (Conteudo c : conteudos) {
                printer.printRecord(
                        CsvUtils.neutralizarFormula(c.getTitulo()),
                        c.getTipo().name(),
                        c.getUrl(),
                        c.getPlanoMinimo().name(),
                        c.getDuracaoMinutos() == null ? "" : c.getDuracaoMinutos(),
                        c.isPublicado());
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
        List<Conteudo> paraSalvar = new ArrayList<>();
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
        conteudoRepository.saveAll(paraSalvar);
        return new ImportResultResponse(registros.size(), paraSalvar.size(), List.of());
    }

    private Conteudo parseLinha(CSVRecord registro) {
        String titulo = registro.get("titulo");
        CsvUtils.exigirNaoVazio(titulo, "Título");
        CsvUtils.exigirTamanhoMaximo(titulo, 255, "Título");

        TipoConteudo tipo = CsvUtils.parseEnum(TipoConteudo.class, registro.get("tipo"), "Tipo");

        String url = registro.get("url");
        CsvUtils.exigirUrl(url, "URL");

        Plano planoMinimo = CsvUtils.parseEnumOpcional(Plano.class, registro.get("planoMinimo"), "Plano mínimo", Plano.GRATUITO);

        Integer duracaoMinutos = CsvUtils.parseIntOpcional(registro.get("duracaoMinutos"), "Duração (minutos)");

        Conteudo conteudo = new Conteudo(titulo.trim(), tipo, url.trim(), planoMinimo);
        conteudo.definirDuracaoMinutos(duracaoMinutos);
        return conteudo;
    }
}
