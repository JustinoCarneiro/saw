package com.sawhub.hub.evento;

import com.sawhub.hub.common.CsvUtils;
import com.sawhub.hub.common.dto.ImportErro;
import com.sawhub.hub.common.dto.ImportResultResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** M23 — export/import CSV de {@link Evento}. Import cria eventos como PROGRAMADO. */
@Service
public class EventoCsvService {

    private static final String FUSO = "America/Sao_Paulo";
    private static final String[] CABECALHO_EXPORT = {
            "titulo", "tipo", "tema", "dataHora", "local", "linkOnline", "vagas", "vagasOcupadas", "status"
    };
    private static final String[] CABECALHO_IMPORT = {
            "titulo", "tipo", "tema", "dataHora", "local", "linkOnline", "vagas"
    };
    private static final int LIMITE_LINHAS = 5000;

    private final EventoRepository eventoRepository;

    public EventoCsvService(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    public String exportar(TipoEvento tipo, StatusEvento status) {
        List<Evento> eventos = eventoRepository.findAll().stream()
                .filter(e -> tipo == null || e.getTipo() == tipo)
                .filter(e -> status == null || e.getStatus() == status)
                .toList();
        StringWriter destino = new StringWriter();
        CSVFormat formato = CSVFormat.Builder.create().setDelimiter(';').setHeader(CABECALHO_EXPORT).build();
        try (CSVPrinter printer = new CSVPrinter(destino, formato)) {
            for (Evento e : eventos) {
                printer.printRecord(
                        CsvUtils.neutralizarFormula(e.getTitulo()),
                        e.getTipo().name(),
                        CsvUtils.neutralizarFormula(e.getTema() == null ? "" : e.getTema()),
                        CsvUtils.formatarInstant(e.getDataHora(), FUSO),
                        CsvUtils.neutralizarFormula(e.getLocal() == null ? "" : e.getLocal()),
                        e.getLinkOnline() == null ? "" : e.getLinkOnline(),
                        e.getVagas() == null ? "" : e.getVagas(),
                        e.getVagasOcupadas(),
                        e.getStatus().name());
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
        List<Evento> paraSalvar = new ArrayList<>();
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
        eventoRepository.saveAll(paraSalvar);
        return new ImportResultResponse(registros.size(), paraSalvar.size(), List.of());
    }

    private Evento parseLinha(CSVRecord registro) {
        String titulo = registro.get("titulo");
        CsvUtils.exigirNaoVazio(titulo, "Título");
        CsvUtils.exigirTamanhoMaximo(titulo, 255, "Título");

        TipoEvento tipo = CsvUtils.parseEnum(TipoEvento.class, registro.get("tipo"), "Tipo");

        String tema = registro.get("tema");
        CsvUtils.exigirTamanhoMaximo(tema, 255, "Tema");

        Instant dataHora = CsvUtils.parseInstant(registro.get("dataHora"), FUSO);

        String local = registro.get("local");
        CsvUtils.exigirTamanhoMaximo(local, 255, "Local");

        String linkOnline = registro.get("linkOnline");
        CsvUtils.exigirUrlOpcional(linkOnline, "Link online");

        Integer vagas = CsvUtils.parseIntOpcional(registro.get("vagas"), "Vagas");

        return new Evento(titulo.trim(), tipo,
                tema == null || tema.isBlank() ? null : tema.trim(),
                dataHora,
                local == null || local.isBlank() ? null : local.trim(),
                linkOnline == null || linkOnline.isBlank() ? null : linkOnline.trim(),
                vagas);
    }
}
