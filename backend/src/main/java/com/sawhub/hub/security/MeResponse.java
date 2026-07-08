package com.sawhub.hub.security;

import com.sawhub.hub.team.AreaModuloMatrix;
import java.util.List;
import java.util.UUID;

public record MeResponse(UUID id, String nome, String email, String perfil, String area, List<String> modulosPermitidos) {

    public static MeResponse from(AppUserPrincipal principal) {
        List<String> modulos = principal.getArea() == null
                ? List.of()
                : AreaModuloMatrix.allowedModulos(principal.getArea()).stream().map(Enum::name).toList();
        return new MeResponse(
                principal.getUsuarioId(),
                principal.getNome(),
                principal.getUsername(),
                principal.getPerfil().name(),
                principal.getArea() == null ? null : principal.getArea().name(),
                modulos
        );
    }
}
