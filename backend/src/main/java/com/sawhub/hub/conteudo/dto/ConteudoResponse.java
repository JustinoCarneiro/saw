package com.sawhub.hub.conteudo.dto;

import com.sawhub.hub.conteudo.Conteudo;
import com.sawhub.hub.conteudo.TipoConteudo;
import com.sawhub.hub.mentorado.Plano;
import java.time.Instant;
import java.util.UUID;

public record ConteudoResponse(
        UUID id,
        String titulo,
        TipoConteudo tipo,
        String url,
        Plano planoMinimo,
        boolean publicado,
        Instant criadoEm,
        Integer duracaoMinutos
) {
    public static ConteudoResponse from(Conteudo c) {
        return new ConteudoResponse(c.getId(), c.getTitulo(), c.getTipo(), c.getUrl(), c.getPlanoMinimo(),
                c.isPublicado(), c.getCriadoEm(), c.getDuracaoMinutos());
    }
}
