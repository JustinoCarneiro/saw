package com.sawhub.hub.comercial;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VendaIngressoRepository extends JpaRepository<VendaIngresso, UUID> {
    List<VendaIngresso> findByLeadId(UUID leadId);

    // M25 — achado do revisor-seguranca: ComercialDashboardService.resumoVendaIngresso() lê
    // venda.getLead().getValorTotalVenda() fora de transação (open-in-view=false); sem o
    // LEFT JOIN FETCH, `lead` fica como proxy LAZY não inicializado e estoura
    // LazyInitializationException — mesmo raciocínio já usado em LeadRepository.buscarComFiltro.
    @Query("SELECT v FROM VendaIngresso v LEFT JOIN FETCH v.lead WHERE v.evento.id = :eventoId")
    List<VendaIngresso> buscarPorEventoIdComLead(@Param("eventoId") UUID eventoId);
}
