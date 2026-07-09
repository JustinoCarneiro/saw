package com.sawhub.hub.loja;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// LEFT JOIN FETCH em itens/produto em TODA query de propósito, mesma disciplina do achado ao vivo
// do M12 (LazyInitializationException numa query de Mentoria que não tinha o fetch join que outra
// tinha) — toda resposta deste módulo lê pedido.getItens(), então nenhuma query aqui pode deixar
// essa coleção como proxy não inicializado (open-in-view=false).
public interface PedidoRepository extends JpaRepository<Pedido, UUID> {

    @Query("SELECT DISTINCT p FROM Pedido p LEFT JOIN FETCH p.itens i LEFT JOIN FETCH i.produto "
            + "WHERE p.mentorado.id = :mentoradoId AND p.status = :status")
    Optional<Pedido> buscarPorMentoradoEStatus(@Param("mentoradoId") UUID mentoradoId, @Param("status") StatusPedido status);

    @Query("SELECT DISTINCT p FROM Pedido p LEFT JOIN FETCH p.itens i LEFT JOIN FETCH i.produto "
            + "WHERE p.mentorado.id = :mentoradoId AND p.status <> :statusExcluido ORDER BY p.criadoEm DESC")
    List<Pedido> buscarHistorico(@Param("mentoradoId") UUID mentoradoId, @Param("statusExcluido") StatusPedido statusExcluido);

    @Query("SELECT DISTINCT p FROM Pedido p LEFT JOIN FETCH p.itens i LEFT JOIN FETCH i.produto WHERE p.id = :id")
    Optional<Pedido> buscarPorIdComItens(@Param("id") UUID id);

    // Admin (H8.4) — visão de todos os pedidos, mais recente primeiro, com fetch join de
    // mentorado (LEFT JOIN FETCH m.mentorado) pro nome aparecer sem 2ª query.
    @Query("SELECT DISTINCT p FROM Pedido p LEFT JOIN FETCH p.itens i LEFT JOIN FETCH i.produto "
            + "LEFT JOIN FETCH p.mentorado m "
            + "WHERE (:status IS NULL OR p.status = :status) "
            + "ORDER BY p.criadoEm DESC")
    List<Pedido> buscarParaAdmin(@Param("status") StatusPedido status);
}
