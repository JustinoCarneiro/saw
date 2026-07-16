package com.sawhub.hub.meta;

import com.sawhub.hub.common.CsvUtils;
import com.sawhub.hub.common.dto.ImportErro;
import com.sawhub.hub.common.dto.ImportResultResponse;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
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

/** M23 — export/import CSV de {@link Meta}. Import cria metas em nome de mentorados (via e-mail).
 * Duas passadas: a primeira valida sem mutar, a segunda cria. Ver justificativa no
 * {@code MentoradoCsvService} (M22). */
@Service
public class MetaCsvService {

    private static final String[] CABECALHO_EXPORT = {
            "emailMentorado", "nomeMentorado", "titulo", "descricao", "prazo", "progressoPct", "status"
    };
    private static final String[] CABECALHO_IMPORT = {"emailMentorado", "titulo", "descricao", "prazo"};
    private static final int LIMITE_LINHAS = 5000;

    private final MetaRepository metaRepository;
    private final MentoradoRepository mentoradoRepository;
    private final UsuarioRepository usuarioRepository;

    public MetaCsvService(MetaRepository metaRepository, MentoradoRepository mentoradoRepository,
                           UsuarioRepository usuarioRepository) {
        this.metaRepository = metaRepository;
        this.mentoradoRepository = mentoradoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public String exportar() {
        List<Meta> metas = metaRepository.findAll();
        StringWriter destino = new StringWriter();
        CSVFormat formato = CSVFormat.Builder.create().setDelimiter(';').setHeader(CABECALHO_EXPORT).build();
        try (CSVPrinter printer = new CSVPrinter(destino, formato)) {
            for (Meta m : metas) {
                printer.printRecord(
                        m.getMentorado().getUsuario().getEmail(),
                        CsvUtils.neutralizarFormula(m.getMentorado().getNome()),
                        CsvUtils.neutralizarFormula(m.getTitulo()),
                        CsvUtils.neutralizarFormula(m.getDescricao() == null ? "" : m.getDescricao()),
                        CsvUtils.formatarData(m.getPrazo()),
                        m.getProgressoPct(),
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

        // Primeira passada — só validação/leitura, nenhuma entidade é mutada.
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

        // Segunda passada — cria as metas.
        List<Meta> paraSalvar = new ArrayList<>();
        for (LinhaValidada v : validas) {
            paraSalvar.add(new Meta(v.mentorado(), v.titulo(), v.descricao(), v.prazo()));
        }
        metaRepository.saveAll(paraSalvar);
        return new ImportResultResponse(registros.size(), paraSalvar.size(), List.of());
    }

    private LinhaValidada validarLinha(CSVRecord registro) {
        String email = CsvUtils.normalizarEmail(registro.get("emailMentorado"));
        Mentorado mentorado = usuarioRepository.findByEmail(email)
                .flatMap(mentoradoRepository::findByUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Mentorado com e-mail \"" + email + "\" não encontrado."));

        String titulo = registro.get("titulo");
        CsvUtils.exigirNaoVazio(titulo, "Título");

        String descricao = registro.get("descricao");

        LocalDate prazo = CsvUtils.parseData(registro.get("prazo"));

        return new LinhaValidada(mentorado, titulo.trim(),
                descricao == null || descricao.isBlank() ? null : descricao.trim(), prazo);
    }

    private record LinhaValidada(Mentorado mentorado, String titulo, String descricao, LocalDate prazo) {
    }
}
