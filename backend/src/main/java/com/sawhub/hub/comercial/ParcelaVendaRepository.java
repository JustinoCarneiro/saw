package com.sawhub.hub.comercial;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParcelaVendaRepository extends JpaRepository<ParcelaVenda, UUID> {
    List<ParcelaVenda> findByLeadId(UUID leadId);
}
