package com.sawhub.hub.loja;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProdutoRepository extends JpaRepository<Produto, UUID> {

    // CAST(:busca AS string) força o tipo do parâmetro quando nulo — mesmo achado de inferência
    // de tipo do Postgres já documentado em MentoradoRepository.buscarComFiltro.
    @Query("SELECT p FROM Produto p "
            + "WHERE (:categoria IS NULL OR p.categoria = :categoria) "
            + "AND (:publicado IS NULL OR p.publicado = :publicado) "
            + "AND (:destaque IS NULL OR p.destaque = :destaque) "
            + "AND (:busca IS NULL OR LOWER(p.titulo) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))) "
            + "ORDER BY p.criadoEm DESC")
    List<Produto> buscarComFiltro(@Param("categoria") CategoriaProduto categoria, @Param("publicado") Boolean publicado,
                                   @Param("destaque") Boolean destaque, @Param("busca") String busca);
}
