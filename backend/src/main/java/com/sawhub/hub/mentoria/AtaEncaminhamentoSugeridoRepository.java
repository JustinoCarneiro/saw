package com.sawhub.hub.mentoria;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtaEncaminhamentoSugeridoRepository extends JpaRepository<AtaEncaminhamentoSugerido, UUID> {
    List<AtaEncaminhamentoSugerido> findByAtaIdOrderByTituloAsc(UUID ataId);
}
