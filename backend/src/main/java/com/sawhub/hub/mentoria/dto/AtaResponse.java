package com.sawhub.hub.mentoria.dto;

import com.sawhub.hub.mentoria.Ata;
import com.sawhub.hub.mentoria.AtaEncaminhamentoSugerido;
import com.sawhub.hub.mentoria.StatusAta;
import com.sawhub.hub.mentoria.StatusProcessamentoAta;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AtaResponse(
        UUID id,
        UUID mentoriaId,
        String transcricao,
        String resumo,
        StatusProcessamentoAta statusProcessamento,
        StatusAta status,
        String erroProcessamento,
        Instant publicadaEm,
        List<SugestaoResponse> sugestoes
) {
    public static AtaResponse from(Ata ata, List<AtaEncaminhamentoSugerido> sugestoes) {
        return new AtaResponse(ata.getId(), ata.getMentoria().getId(), ata.getTranscricao(), ata.getResumo(),
                ata.getStatusProcessamento(), ata.getStatus(), ata.getErroProcessamento(), ata.getPublicadaEm(),
                sugestoes.stream().map(SugestaoResponse::from).toList());
    }
}
