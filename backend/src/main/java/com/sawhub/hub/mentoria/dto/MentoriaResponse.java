package com.sawhub.hub.mentoria.dto;

import com.sawhub.hub.mentoria.Mentoria;
import com.sawhub.hub.mentoria.StatusMentoria;
import com.sawhub.hub.mentoria.TipoMentoria;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MentoriaResponse(
        UUID id,
        TipoMentoria tipo,
        MentorResumo mentor,
        List<MentoradoResumo> mentorados,
        Instant dataHora,
        Integer duracaoMin,
        String linkOnline,
        String local,
        StatusMentoria status,
        List<MaterialResumoResponse> materiaisRecomendados
) {
    public record MentorResumo(UUID id, String nome) {
    }

    public record MentoradoResumo(UUID id, String nome) {
    }

    public static MentoriaResponse from(Mentoria m) {
        var mentorados = m.getMentorados().stream()
                .map(mt -> new MentoradoResumo(mt.getId(), mt.getNome()))
                .sorted((a, b) -> a.nome().compareTo(b.nome()))
                .toList();
        // Sem filtro de publicado/plano aqui de propósito — visão do Admin, que já cura o
        // conteúdo (diferente de MentoriaMentoradoResponse, mentee-facing, ver ROADMAP.md M12).
        var materiais = m.getMateriaisRecomendados().stream()
                .map(MaterialResumoResponse::from)
                .sorted((a, b) -> a.titulo().compareTo(b.titulo()))
                .toList();
        return new MentoriaResponse(m.getId(), m.getTipo(), new MentorResumo(m.getMentor().getId(), m.getMentor().getNome()),
                mentorados, m.getDataHora(), m.getDuracaoMin(), m.getLinkOnline(), m.getLocal(), m.getStatus(), materiais);
    }
}
