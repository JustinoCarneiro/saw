package com.sawhub.hub.team;

import com.sawhub.hub.security.Usuario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColaboradorRepository extends JpaRepository<Colaborador, UUID> {
    Optional<Colaborador> findByUsuario(Usuario usuario);

    // Pass transversal de pgcrypto (Fase 5): nome virou bytea criptografado (ver Colaborador.java)
    // — ORDER BY nome no SQL ordenaria pelos bytes do texto cifrado, não pelo nome real. Os
    // chamadores agora ordenam em memória Java depois do fetch (lista pequena, time interno) —
    // findAll() já vem do JpaRepository, não precisa de método próprio.
    List<Colaborador> findAllByArea(Area area);
}
