package com.sawhub.hub.team;

import com.sawhub.hub.security.Usuario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColaboradorRepository extends JpaRepository<Colaborador, UUID> {
    Optional<Colaborador> findByUsuario(Usuario usuario);

    List<Colaborador> findAllByOrderByNomeAsc();

    List<Colaborador> findAllByAreaOrderByNomeAsc(Area area);
}
