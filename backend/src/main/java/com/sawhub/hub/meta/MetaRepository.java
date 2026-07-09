package com.sawhub.hub.meta;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MetaRepository extends JpaRepository<Meta, UUID> {

    // status nulo = "Todas" (H3.2) — mesmo padrão de filtro opcional já usado em
    // MentoriaRepository.buscarPorStatus.
    @Query("SELECT m FROM Meta m WHERE m.mentorado.id = :mentoradoId "
            + "AND (:status IS NULL OR m.status = :status) "
            + "ORDER BY m.prazo ASC")
    List<Meta> buscarPorMentorado(@Param("mentoradoId") UUID mentoradoId, @Param("status") StatusMeta status);
}
