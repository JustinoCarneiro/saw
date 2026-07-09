package com.sawhub.hub.conteudo.dto;

import com.sawhub.hub.conteudo.Conteudo;
import com.sawhub.hub.conteudo.ConteudoMentorado;
import com.sawhub.hub.conteudo.TipoConteudo;
import com.sawhub.hub.mentorado.Plano;
import java.time.Instant;
import java.util.UUID;

public record ConteudoMentoradoResponse(
        UUID id,
        String titulo,
        TipoConteudo tipo,
        String url,
        Plano planoMinimo,
        boolean publicado,
        Instant criadoEm,
        boolean favorito,
        boolean assistido
) {
    public static ConteudoMentoradoResponse from(Conteudo c, ConteudoMentorado cm) {
        return new ConteudoMentoradoResponse(
                c.getId(),
                c.getTitulo(),
                c.getTipo(),
                c.getUrl(),
                c.getPlanoMinimo(),
                c.isPublicado(),
                c.getCriadoEm(),
                cm != null && cm.isFavorito(),
                cm != null && cm.isAssistido()
        );
    }
}
