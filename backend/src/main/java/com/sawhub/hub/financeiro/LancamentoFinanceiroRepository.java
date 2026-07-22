package com.sawhub.hub.financeiro;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LancamentoFinanceiroRepository extends JpaRepository<LancamentoFinanceiro, UUID> {

    // JOIN FETCH de propósito: `categoria` é LAZY (correto, evita over-fetch em outros contextos),
    // mas todo consumidor destas queries acaba lendo campos de categoria (nome, grupoDre,
    // origemReceita) fora da transação — sem o JOIN FETCH aqui, dá LazyInitializationException
    // já que o projeto roda com `spring.jpa.open-in-view: false` de propósito. LEFT JOIN FETCH em
    // `evento` pelo mesmo motivo (change request "evento no financeiro").
    @Query("SELECT l FROM LancamentoFinanceiro l JOIN FETCH l.categoria LEFT JOIN FETCH l.evento "
            + "WHERE l.dataCompetencia BETWEEN :de AND :ate ORDER BY l.dataCompetencia DESC")
    List<LancamentoFinanceiro> findByDataCompetenciaBetweenOrderByDataCompetenciaDesc(
            @Param("de") LocalDate de, @Param("ate") LocalDate ate);

    /** DRE (H14.2) e dashboard de faturamento (H14.3) só contam o que de fato aconteceu —
     * PREVISTO é fluxo de caixa futuro, não resultado do período. */
    @Query("SELECT l FROM LancamentoFinanceiro l JOIN FETCH l.categoria LEFT JOIN FETCH l.evento "
            + "WHERE l.status = :status AND l.dataCompetencia BETWEEN :de AND :ate")
    List<LancamentoFinanceiro> findByStatusAndDataCompetenciaBetween(
            @Param("status") StatusLancamento status, @Param("de") LocalDate de, @Param("ate") LocalDate ate);

    /** M26 — filtro de `GET /admin/financeiro/lancamentos` (por `dataCompetencia`, sentinela pro
     * período "desligado" — mesmo raciocínio de {@link #buscarComFiltroPorVencimento}, Postgres
     * não infere tipo de `LocalDate` nulo numa query com tantas colunas `bytea` de pgcrypto ao
     * redor). `status`/`categoriaId`/`eventoId` nulos desligam o respectivo filtro. */
    @Query("SELECT l FROM LancamentoFinanceiro l JOIN FETCH l.categoria LEFT JOIN FETCH l.evento "
            + "WHERE (:tipo IS NULL OR l.tipo = :tipo) "
            + "AND (:categoriaId IS NULL OR l.categoria.id = :categoriaId) "
            + "AND (:status IS NULL OR l.status = :status) "
            + "AND (:eventoId IS NULL OR l.evento.id = :eventoId) "
            + "AND l.dataCompetencia >= :inicio AND l.dataCompetencia < :fim "
            + "ORDER BY l.dataCompetencia DESC")
    List<LancamentoFinanceiro> buscarComFiltroPorCompetencia(@Param("tipo") TipoLancamento tipo,
            @Param("categoriaId") UUID categoriaId, @Param("status") StatusLancamento status,
            @Param("eventoId") UUID eventoId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    /** M26 (absorvido de {@code ContaPagarReceberRepository.buscarComFiltro}) — filtro de
     * `GET /admin/financeiro/contas`, agora um recorte por `dataVencimento` sobre a mesma tabela
     * (não existe mais entidade própria pra "conta"). Linhas com `dataVencimento IS NULL`
     * (lançamento direto, sem prazo) nunca batem `>= :inicio` (comparação com NULL é sempre falsa
     * em SQL) — ficam de fora deste filtro por construção, sem precisar de `IS NOT NULL` extra. */
    @Query("SELECT l FROM LancamentoFinanceiro l JOIN FETCH l.categoria LEFT JOIN FETCH l.evento "
            + "WHERE (:tipo IS NULL OR l.tipo = :tipo) "
            + "AND (:status IS NULL OR l.status = :status) "
            + "AND (:eventoId IS NULL OR l.evento.id = :eventoId) "
            + "AND l.dataVencimento >= :inicio AND l.dataVencimento < :fim "
            + "ORDER BY l.dataVencimento ASC")
    List<LancamentoFinanceiro> buscarComFiltroPorVencimento(@Param("tipo") TipoLancamento tipo,
            @Param("status") StatusLancamento status, @Param("eventoId") UUID eventoId,
            @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT l FROM LancamentoFinanceiro l JOIN FETCH l.categoria LEFT JOIN FETCH l.evento WHERE l.id = :id")
    Optional<LancamentoFinanceiro> buscarPorIdComEvento(@Param("id") UUID id);

    // VencimentoScheduler (absorvido de ContaPagarReceberRepository) — só PREVISTO com
    // dataVencimento preenchida entra aqui; PARCIAL nunca virou VENCIDO (mesmo comportamento de
    // antes) e lançamentos sem vencimento (dataVencimento NULL) nunca batem, mesmo raciocínio do
    // buscarComFiltroPorVencimento acima.
    List<LancamentoFinanceiro> findByStatusAndDataVencimentoBefore(StatusLancamento status, LocalDate data);

    // Pedido do Marcos (22/07/2026) — resumo do Dashboard financeiro: quantos lançamentos
    // precisam de atenção agora, sem escopo de período (um vencido de 3 meses atrás continua
    // relevante hoje, diferente de faturamentoMensal/DRE que são por ano/mês selecionado).
    long countByStatusIn(List<StatusLancamento> status);
}
