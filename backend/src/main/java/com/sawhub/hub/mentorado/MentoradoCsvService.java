package com.sawhub.hub.mentorado;

import com.sawhub.hub.common.CsvUtils;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

/** M22 — export CSV de {@link Mentorado}. Até o M28 também tinha um {@code importar()} bulk-UPDATE
 * (resolvido por e-mail, 6 campos) — removido no M28 (change request, 21/07/2026, "import único"):
 * era o segundo de dois botões de import confusos na tela; {@link MentoradoDiretoCsvService}
 * passou a cobrir criar-ou-atualizar num fluxo só, com o conjunto completo de campos. */
@Service
public class MentoradoCsvService {

    private static final String[] CABECALHO = {"email", "nome", "negocio", "tipoContrato", "status"};

    private final MentoradoRepository mentoradoRepository;

    public MentoradoCsvService(MentoradoRepository mentoradoRepository) {
        this.mentoradoRepository = mentoradoRepository;
    }

    public String exportar(StatusMentorado status, String busca) {
        List<Mentorado> mentorados = mentoradoRepository.buscarComFiltro(status, busca);
        StringWriter destino = new StringWriter();
        CSVFormat formato = CSVFormat.Builder.create().setDelimiter(';').setHeader(CABECALHO).build();
        try (CSVPrinter printer = new CSVPrinter(destino, formato)) {
            for (Mentorado m : mentorados) {
                printer.printRecord(
                        m.getUsuario().getEmail(),
                        CsvUtils.neutralizarFormula(m.getNome()),
                        CsvUtils.neutralizarFormula(m.getNegocio() == null ? "" : m.getNegocio()),
                        m.getTipoContrato() == null ? "" : m.getTipoContrato().name(),
                        m.getStatus().name());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gerar o CSV.", e);
        }
        return destino.toString();
    }
}
