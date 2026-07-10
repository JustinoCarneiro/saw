package com.sawhub.hub.aviso;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvisoRepository extends JpaRepository<Aviso, UUID> {

    List<Aviso> findAllByOrderByCriadoEmDesc();
}
