package com.sawhub.hub.mentorado.dto;

import com.sawhub.hub.mentorado.Encaminhamento;
import com.sawhub.hub.mentorado.Prioridade;
import com.sawhub.hub.mentorado.StatusTarefa;
import java.time.LocalDate;
import java.util.UUID;

// Fase 5 (H4.6) — visão admin de todas as tarefas/encaminhamentos, cruzando mentorados (mesmo
// gap do MetaAdminResponse: só existia export/import CSV cegos, sem tela pra conferir o resultado).
public record EncaminhamentoAdminResponse(
        UUID id,
        UUID mentoradoId,
        String mentoradoNome,
        String titulo,
        Integer peso,
        StatusTarefa status,
        LocalDate prazo,
        Prioridade prioridade
) {
    public static EncaminhamentoAdminResponse from(Encaminhamento e) {
        return new EncaminhamentoAdminResponse(e.getId(), e.getMentorado().getId(), e.getMentorado().getNome(),
                e.getTitulo(), e.getPeso(), e.getStatus(), e.getPrazo(), e.getPrioridade());
    }
}
