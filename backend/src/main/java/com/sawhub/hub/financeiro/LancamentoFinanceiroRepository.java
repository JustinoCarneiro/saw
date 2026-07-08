package com.sawhub.hub.financeiro;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LancamentoFinanceiroRepository extends JpaRepository<LancamentoFinanceiro, UUID> {

    // JOIN FETCH de propósito: `categoria` é LAZY (correto, evita over-fetch em outros contextos),
    // mas todo consumidor destas duas queries acaba lendo campos de categoria (nome, grupoDre,
    // origemReceita) fora da transação — sem o JOIN FETCH aqui, dá LazyInitializationException
    // já que o projeto roda com `spring.jpa.open-in-view: false` de propósito.
    @Query("SELECT l FROM LancamentoFinanceiro l JOIN FETCH l.categoria "
            + "WHERE l.dataCompetencia BETWEEN :de AND :ate ORDER BY l.dataCompetencia DESC")
    List<LancamentoFinanceiro> findByDataCompetenciaBetweenOrderByDataCompetenciaDesc(
            @Param("de") LocalDate de, @Param("ate") LocalDate ate);

    /** DRE (H14.2) e dashboard de faturamento (H14.3) só contam o que de fato aconteceu —
     * PREVISTO é fluxo de caixa futuro, não resultado do período. */
    @Query("SELECT l FROM LancamentoFinanceiro l JOIN FETCH l.categoria "
            + "WHERE l.status = :status AND l.dataCompetencia BETWEEN :de AND :ate")
    List<LancamentoFinanceiro> findByStatusAndDataCompetenciaBetween(
            @Param("status") StatusLancamento status, @Param("de") LocalDate de, @Param("ate") LocalDate ate);
}
