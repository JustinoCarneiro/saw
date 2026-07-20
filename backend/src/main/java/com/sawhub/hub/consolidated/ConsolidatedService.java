package com.sawhub.hub.consolidated;

import com.sawhub.hub.consolidated.dto.ConsolidatedSummaryResponse;
import com.sawhub.hub.consolidated.dto.MentoradoConsolidadoResponse;
import com.sawhub.hub.consolidated.dto.MentoradoConsolidadoRow;
import com.sawhub.hub.consolidated.dto.RankingFaturamentoResponse;
import com.sawhub.hub.mentoria.PresencaMentoriaRepository;
import com.sawhub.hub.mentoria.dto.FrequenciaMentoriaRow;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ConsolidatedService {

    private final ConsolidatedRepository consolidatedRepository;
    private final PresencaMentoriaRepository presencaMentoriaRepository;

    public ConsolidatedService(ConsolidatedRepository consolidatedRepository,
                                PresencaMentoriaRepository presencaMentoriaRepository) {
        this.consolidatedRepository = consolidatedRepository;
        this.presencaMentoriaRepository = presencaMentoriaRepository;
    }

    public List<MentoradoConsolidadoResponse> listarMentorados() {
        Map<UUID, Integer> frequenciaPorMentorado = frequenciaPorMentorado();
        return consolidatedRepository.buscarConsolidado().stream()
                .map(row -> MentoradoConsolidadoResponse.from(row, frequenciaPorMentorado.get(row.id())))
                .sorted(Comparator.comparing(MentoradoConsolidadoResponse::nome))
                .toList();
    }

    // E17/M27 — % de presença confirmada sobre o total de mentorias GRUPO/REALIZADA de que o
    // mentorado participou; ausente do mapa (não 0) se ele nunca participou de nenhuma, ver
    // ROADMAP.md § "Blueprint (M27)".
    private Map<UUID, Integer> frequenciaPorMentorado() {
        return presencaMentoriaRepository.buscarFrequencia().stream()
                .collect(Collectors.toMap(FrequenciaMentoriaRow::mentoradoId, this::pctFrequencia));
    }

    private int pctFrequencia(FrequenciaMentoriaRow row) {
        return row.totalMentoriasGrupo() == 0 ? 0
                : (int) Math.round(row.presencasConfirmadas() * 100.0 / row.totalMentoriasGrupo());
    }

    public ConsolidatedSummaryResponse resumo() {
        List<MentoradoConsolidadoResponse> mentorados = listarMentorados();
        int emDia = (int) mentorados.stream().filter(m -> m.status().equals("EM_DIA")).count();
        int atencao = (int) mentorados.stream().filter(m -> m.status().equals("ATENCAO")).count();
        int atrasado = (int) mentorados.stream().filter(m -> m.status().equals("ATRASADO")).count();
        int progressoMedio = mentorados.isEmpty() ? 0
                : (int) Math.round(mentorados.stream().mapToInt(MentoradoConsolidadoResponse::progressoPct).average().orElse(0));
        return new ConsolidatedSummaryResponse(mentorados.size(), emDia, atencao, atrasado, progressoMedio);
    }

    public List<RankingFaturamentoResponse> rankingFaturamento(int topN) {
        List<MentoradoConsolidadoRow> rows = consolidatedRepository.buscarConsolidado();
        List<MentoradoConsolidadoRow> ordenado = rows.stream()
                .sorted(Comparator.comparing(MentoradoConsolidadoRow::crescimentoFaturamentoPct, Comparator.reverseOrder()))
                .limit(topN)
                .toList();
        List<RankingFaturamentoResponse> ranking = new java.util.ArrayList<>();
        for (int i = 0; i < ordenado.size(); i++) {
            MentoradoConsolidadoRow row = ordenado.get(i);
            BigDecimal cresc = row.crescimentoFaturamentoPct() == null ? BigDecimal.ZERO : row.crescimentoFaturamentoPct();
            ranking.add(new RankingFaturamentoResponse(i + 1, row.nome(), cresc));
        }
        return ranking;
    }
}
