package com.sawhub.hub.mentoria.dto;

import com.sawhub.hub.team.Colaborador;
import java.util.UUID;

public record MentorResumoResponse(UUID id, String nome) {
    public static MentorResumoResponse from(Colaborador c) {
        return new MentorResumoResponse(c.getId(), c.getNome());
    }
}
