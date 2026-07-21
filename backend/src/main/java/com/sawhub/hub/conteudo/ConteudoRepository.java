package com.sawhub.hub.conteudo;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConteudoRepository extends JpaRepository<Conteudo, UUID> {

    @Query("SELECT c FROM Conteudo c "
            + "WHERE (:tipo IS NULL OR c.tipo = :tipo) "
            + "AND (:publicado IS NULL OR c.publicado = :publicado) "
            + "ORDER BY c.criadoEm DESC")
    List<Conteudo> buscarComFiltro(@Param("tipo") TipoConteudo tipo, @Param("publicado") Boolean publicado);
}
