package com.sawhub.hub.security;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    // H1.4 — ao redefinir com sucesso, invalida os outros tokens pendentes do mesmo usuário
    // (Suposição 3 do Blueprint M18): se o usuário pediu reset várias vezes, só o primeiro uso vale.
    List<PasswordResetToken> findByUsuarioIdAndUsadoEmIsNull(UUID usuarioId);
}
