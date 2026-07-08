package com.sawhub.hub.mentorado;

import com.sawhub.hub.security.Usuario;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MentoradoRepository extends JpaRepository<Mentorado, UUID> {
    Optional<Mentorado> findByUsuario(Usuario usuario);
}
