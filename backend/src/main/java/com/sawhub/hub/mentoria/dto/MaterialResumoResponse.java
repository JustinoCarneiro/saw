package com.sawhub.hub.mentoria.dto;

import com.sawhub.hub.conteudo.Conteudo;
import com.sawhub.hub.conteudo.TipoConteudo;
import java.util.UUID;

/** H5.2 (M12) — forma compartilhada entre {@link MentoriaResponse} (Admin, sem filtro) e
 * {@code MentoriaMentoradoResponse} (mentee-facing, filtrado por publicado/plano). */
public record MaterialResumoResponse(UUID id, String titulo, TipoConteudo tipo, String url) {
    public static MaterialResumoResponse from(Conteudo c) {
        return new MaterialResumoResponse(c.getId(), c.getTitulo(), c.getTipo(), c.getUrl());
    }
}
