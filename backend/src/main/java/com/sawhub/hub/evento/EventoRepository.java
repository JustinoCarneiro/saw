package com.sawhub.hub.evento;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoRepository extends JpaRepository<Evento, UUID> {

    @Query("SELECT e FROM Evento e "
            + "WHERE (:tipo IS NULL OR e.tipo = :tipo) "
            + "AND (:status IS NULL OR e.status = :status) "
            + "ORDER BY e.dataHora ASC")
    List<Evento> buscarComFiltro(@Param("tipo") TipoEvento tipo, @Param("status") StatusEvento status);
}
