package com.sawhub.hub.mentorado.dto;

import com.sawhub.hub.mentorado.Mentorado;
import java.util.UUID;

/** Resposta específica de "criar a partir do lead" — carrega a senha temporária **uma única vez**
 * (não fica recuperável depois, só o hash é persistido). Não existe envio de e-mail no MVP ainda,
 * então o Admin precisa repassar essa senha manualmente ao mentorado. */
public record MentoradoCriadoResponse(UUID id, String nome, String email, String senhaTemporaria) {
    public static MentoradoCriadoResponse from(Mentorado m, String senhaTemporaria) {
        return new MentoradoCriadoResponse(m.getId(), m.getNome(), m.getUsuario().getEmail(), senhaTemporaria);
    }
}
