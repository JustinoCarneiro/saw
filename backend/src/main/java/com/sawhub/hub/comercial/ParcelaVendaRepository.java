package com.sawhub.hub.comercial;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParcelaVendaRepository extends JpaRepository<ParcelaVenda, UUID> {
    List<ParcelaVenda> findByLeadId(UUID leadId);

    // LEFT JOIN FETCH em lancamento (change request 17/07/2026, "conciliação"; M26 renomeou de
    // contaPagarReceber) — mesmo raciocínio de sempre neste projeto: ConciliacaoService lê
    // lancamento.getStatus()/getValorPago() fora da transação original, um proxy LAZY não
    // inicializado estouraria LazyInitializationException (open-in-view=false).
    @Query("SELECT p FROM ParcelaVenda p LEFT JOIN FETCH p.lancamento WHERE p.lead.id = :leadId")
    List<ParcelaVenda> buscarPorLeadIdComConta(@Param("leadId") UUID leadId);
}
