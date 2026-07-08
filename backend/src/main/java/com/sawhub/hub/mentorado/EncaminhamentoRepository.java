package com.sawhub.hub.mentorado;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EncaminhamentoRepository extends JpaRepository<Encaminhamento, UUID> {
    List<Encaminhamento> findByMentoradoIdOrderByCriadoEmDesc(UUID mentoradoId);
}
