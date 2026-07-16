package com.sawhub.hub.aviso;

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

/** M23 — export/import CSV de {@link Aviso}. Import cria = publica (sem rascunho, M17). */
@Service
public class AvisoCsvService {

    private static final String[] CABECALHO = {"titulo", "descricao", "categoria", "planoMinimo"};
    private static final int LIMITE_LINHAS = 5000;

    private final AvisoRepository avisoRepository;

    public AvisoCsvService(AvisoRepository avisoRepository) {
        this.avisoRepository = avisoRepository;
    }

    public String exportar() {
        List<Aviso> avisos = avisoRepository.findAll();
        StringWriter destino = new StringWriter();
        CSVFormat formato = CSVFormat.Builder.create().setDelimiter(';').setHeader(CABECALHO).build();
        try (CSVPrinter printer = new CSVPrinter(destino, formato)) {
            for (Aviso a : avisos) {
                printer.printRecord(
                        CsvUtils.neutralizarFormula(a.getTitulo()),
                        CsvUtils.neutralizarFormula(a.getDescricao()),
                        a.getCategoria().name(),
                        a.getPlanoMinimo().name());
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

        List<ImportErro> erros = new ArrayList<>();
        List<Aviso> paraSalvar = new ArrayList<>();
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
        avisoRepository.saveAll(paraSalvar);
        return new ImportResultResponse(registros.size(), paraSalvar.size(), List.of());
    }

    private Aviso parseLinha(CSVRecord registro) {
        String titulo = registro.get("titulo");
        CsvUtils.exigirNaoVazio(titulo, "Título");
        CsvUtils.exigirTamanhoMaximo(titulo, 200, "Título");

        String descricao = registro.get("descricao");
        CsvUtils.exigirNaoVazio(descricao, "Descrição");
        CsvUtils.exigirTamanhoMaximo(descricao, 1000, "Descrição");

        CategoriaAviso categoria = CsvUtils.parseEnum(CategoriaAviso.class, registro.get("categoria"), "Categoria");
        Plano planoMinimo = CsvUtils.parseEnum(Plano.class, registro.get("planoMinimo"), "Plano mínimo");

        return new Aviso(titulo.trim(), descricao.trim(), categoria, planoMinimo);
    }
}
