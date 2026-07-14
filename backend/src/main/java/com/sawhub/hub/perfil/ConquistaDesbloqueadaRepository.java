package com.sawhub.hub.perfil;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConquistaDesbloqueadaRepository extends JpaRepository<ConquistaDesbloqueada, UUID> {

    List<ConquistaDesbloqueada> findByMentoradoId(UUID mentoradoId);
}
