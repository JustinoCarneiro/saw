package com.sawhub.hub.meta.dto;

import com.sawhub.hub.meta.Meta;
import com.sawhub.hub.meta.StatusMeta;
import java.time.LocalDate;
import java.util.UUID;

// Fase 5 (H3.4) — visão admin de todas as metas, cruzando mentorados (Meta é self-service, M09,
// sem endpoint de listagem admin até aqui — só export/import CSV cegos, sem tela nenhuma pra
// conferir o resultado).
public record MetaAdminResponse(
        UUID id,
        UUID mentoradoId,
        String mentoradoNome,
        String titulo,
        String descricao,
        LocalDate prazo,
        Integer progressoPct,
        StatusMeta status
) {
    public static MetaAdminResponse from(Meta m) {
        return new MetaAdminResponse(m.getId(), m.getMentorado().getId(), m.getMentorado().getNome(),
                m.getTitulo(), m.getDescricao(), m.getPrazo(), m.getProgressoPct(), m.getStatus());
    }
}
