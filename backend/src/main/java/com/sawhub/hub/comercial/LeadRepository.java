package com.sawhub.hub.comercial;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeadRepository extends JpaRepository<Lead, UUID> {

    // Achado M1 da revisão de segurança: filtro em SQL, não em memória Java — a versão anterior
    // buscava a tabela inteira e filtrava com stream(), ignorando os índices idx_lead_status/
    // idx_lead_vendedor e materializando tudo no heap a cada GET (o formulário público sem rate
    // limit conseguia inflar a tabela e amplificar isso num DoS do próprio endpoint admin).
    // LEFT JOIN FETCH de propósito: `vendedor` é LAZY e nullable (só setado a partir de
    // EM_CONTATO), e o LeadResponse (fora da transação, open-in-view=false) lê vendedor.nome.
    @Query("SELECT l FROM Lead l LEFT JOIN FETCH l.vendedor "
            + "WHERE (:status IS NULL OR l.status = :status) "
            + "AND (:vendedorId IS NULL OR l.vendedor.id = :vendedorId) "
            + "ORDER BY l.criadoEm DESC")
    List<Lead> buscarComFiltro(@Param("status") StatusLead status, @Param("vendedorId") UUID vendedorId);

    // Bug achado ao vivo (verificação de M05 via curl): LeadService.avancar() usava findById()
    // puro, que devolve `vendedor` como proxy LAZY não inicializado. Qualquer transição que não
    // seja a própria atribuição de vendedor (ex.: EM_CONTATO -> PROPOSTA) mantinha esse proxy
    // intocado, e o LeadResponse (fora da transação, open-in-view=false) explodia com
    // LazyInitializationException ao ler vendedor.nome — mesmo raciocínio do LEFT JOIN FETCH
    // já usado em buscarComFiltro.
    @Query("SELECT l FROM Lead l LEFT JOIN FETCH l.vendedor WHERE l.id = :id")
    Optional<Lead> buscarPorIdComVendedor(@Param("id") UUID id);

    long countByStatus(StatusLead status);

    /** H13.1/H13.2 — conversão e "novos mentorados no mês" contam por quando o lead FECHOU/foi
     * PERDIDO, não por quando foi criado (um lead criado em junho e fechado em julho conta em julho). */
    long countByStatusAndDataFechamentoBetween(StatusLead status, Instant de, Instant ate);

    /** H13.3 — realizado do vendedor no período (leads fechados por ele). */
    long countByVendedorIdAndStatusAndDataFechamentoBetween(UUID vendedorId, StatusLead status, Instant de, Instant ate);

    /** M25 (Suposição 7) — "novos mentorados no mês" exclui um produto (venda de ingresso é
     * contabilizada à parte, ver ComercialDashboardService). produtoVenda IS NULL cobre o caminho
     * legado (Lead.fechar(Plano) nunca seta produtoVenda) — continua contando normalmente. Produto
     * é parâmetro, não literal — mesmo padrão do resto do projeto pra enum em JPQL. */
    @Query("SELECT COUNT(l) FROM Lead l WHERE l.status = :status AND l.dataFechamento BETWEEN :de AND :ate "
            + "AND (l.produtoVenda IS NULL OR l.produtoVenda <> :produtoExcluido)")
    long countByStatusAndDataFechamentoBetweenExcluindoProduto(@Param("status") StatusLead status,
            @Param("de") Instant de, @Param("ate") Instant ate, @Param("produtoExcluido") ProdutoVenda produtoExcluido);

    /** M25 (Suposição 7) — mesma exclusão de produto, escopada por vendedor (ranking/H13.3). */
    @Query("SELECT COUNT(l) FROM Lead l WHERE l.vendedor.id = :vendedorId AND l.status = :status "
            + "AND l.dataFechamento BETWEEN :de AND :ate "
            + "AND (l.produtoVenda IS NULL OR l.produtoVenda <> :produtoExcluido)")
    long countByVendedorIdAndStatusAndDataFechamentoBetweenExcluindoProduto(@Param("vendedorId") UUID vendedorId,
            @Param("status") StatusLead status, @Param("de") Instant de, @Param("ate") Instant ate,
            @Param("produtoExcluido") ProdutoVenda produtoExcluido);

    /** Change request 17/07/2026 ("conciliação") — toda venda fechada via {@link Lead#fecharVenda}
     * (nunca via {@link Lead#fechar} legado, que não seta valorTotalVenda). IS NOT NULL numa
     * coluna pgcrypto funciona normal — criptografia não afeta nulidade, só o conteúdo. */
    @Query("SELECT l FROM Lead l WHERE l.valorTotalVenda IS NOT NULL ORDER BY l.dataFechamento DESC")
    List<Lead> buscarComVendaFechada();
}
