package com.sawhub.hub.financeiro;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContaPagarReceberRepository extends JpaRepository<ContaPagarReceber, UUID> {

    // LEFT JOIN FETCH em categoria (M21): achado ao vivo — ContaResponse nunca lia
    // conta.getCategoria(), então o LazyInitializationException fora da transação
    // (open-in-view=false) nunca disparava; ContaCsvService.exportar() é o primeiro consumidor de
    // ContaPagarReceberService.listar() a ler o nome da categoria, mesma classe de achado já
    // documentada no M12 (Mentoria.materiaisRecomendados).
    @Query("SELECT c FROM ContaPagarReceber c LEFT JOIN FETCH c.categoria ORDER BY c.dataVencimento ASC")
    List<ContaPagarReceber> findAllByOrderByDataVencimentoAsc();

    @Query("SELECT c FROM ContaPagarReceber c LEFT JOIN FETCH c.categoria WHERE c.tipo = :tipo ORDER BY c.dataVencimento ASC")
    List<ContaPagarReceber> findByTipoOrderByDataVencimentoAsc(@Param("tipo") TipoConta tipo);

    @Query("SELECT c FROM ContaPagarReceber c LEFT JOIN FETCH c.categoria WHERE c.status = :status ORDER BY c.dataVencimento ASC")
    List<ContaPagarReceber> findByStatusOrderByDataVencimentoAsc(@Param("status") StatusConta status);

    List<ContaPagarReceber> findByStatusAndDataVencimentoBefore(StatusConta status, LocalDate data);
}
