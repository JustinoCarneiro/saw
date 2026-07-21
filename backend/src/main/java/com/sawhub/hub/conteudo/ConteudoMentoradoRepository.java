package com.sawhub.hub.conteudo;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConteudoMentoradoRepository extends JpaRepository<ConteudoMentorado, ConteudoMentoradoId> {

    @Query("""
        SELECT c, cm
        FROM Conteudo c
        LEFT JOIN ConteudoMentorado cm ON cm.conteudo = c AND cm.mentorado.id = :mentoradoId
        WHERE c.publicado = true
        AND (:tipo IS NULL OR c.tipo = :tipo)
        AND (:favorito IS NULL OR cm.favorito = :favorito)
        ORDER BY c.criadoEm DESC
    """)
    List<Object[]> buscarCatalogo(
        @Param("mentoradoId") UUID mentoradoId,
        @Param("tipo") TipoConteudo tipo,
        @Param("favorito") Boolean favorito
    );

    @Query("""
        SELECT c, cm
        FROM Conteudo c
        LEFT JOIN ConteudoMentorado cm ON cm.conteudo = c AND cm.mentorado.id = :mentoradoId
        WHERE c.publicado = true
        AND c.tipo = 'VIDEO'
        ORDER BY c.criadoEm DESC
    """)
    List<Object[]> buscarDicas(
        @Param("mentoradoId") UUID mentoradoId
    );

    // H6.3 — "dias assistidos" agrega por data de dataConsumo (ver ConteudoMentorado), não conta
    // linhas: assistir 3 conteúdos no mesmo dia é 1 dia assistido, não 3.
    List<ConteudoMentorado> findByMentoradoIdAndAssistidoTrue(UUID mentoradoId);

    long countByMentoradoIdAndFavoritoTrue(UUID mentoradoId);

}
