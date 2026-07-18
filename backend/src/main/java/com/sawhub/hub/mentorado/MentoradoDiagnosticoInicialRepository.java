package com.sawhub.hub.mentorado;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MentoradoDiagnosticoInicialRepository extends JpaRepository<MentoradoDiagnosticoInicial, UUID> {
    Optional<MentoradoDiagnosticoInicial> findByMentoradoId(UUID mentoradoId);
}
