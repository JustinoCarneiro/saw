package com.sawhub.hub.atividade;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtividadeLogRepository extends JpaRepository<AtividadeLog, UUID> {

    List<AtividadeLog> findAllByOrderByCriadoEmDesc();
}
