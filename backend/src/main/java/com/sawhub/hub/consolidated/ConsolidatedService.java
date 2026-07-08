package com.sawhub.hub.consolidated;

import com.sawhub.hub.consolidated.dto.ConsolidatedSummaryResponse;
import com.sawhub.hub.consolidated.dto.MentoradoConsolidadoResponse;
import com.sawhub.hub.consolidated.dto.MentoradoConsolidadoRow;
import com.sawhub.hub.consolidated.dto.RankingFaturamentoResponse;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ConsolidatedService {

    private final ConsolidatedRepository consolidatedRepository;

    public ConsolidatedService(ConsolidatedRepository consolidatedRepository) {
        this.consolidatedRepository = consolidatedRepository;
    }

    public List<MentoradoConsolidadoResponse> listarMentorados() {
        return consolidatedRepository.buscarConsolidado().stream()
                .map(MentoradoConsolidadoResponse::from)
                .sorted(Comparator.comparing(MentoradoConsolidadoResponse::nome))
                .toList();
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
