package com.sawhub.hub.consolidated;

import com.sawhub.hub.consolidated.dto.MentoradoConsolidadoRow;
import com.sawhub.hub.mentorado.Mentorado;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Só leitura agregada — não é CRUD de Mentorado (isso é MentoradoRepository), por isso
 * estende {@link Repository} puro em vez de JpaRepository (não traz save/delete/etc.). */
public interface ConsolidatedRepository extends Repository<Mentorado, UUID> {

    @Query("""
            SELECT new com.sawhub.hub.consolidated.dto.MentoradoConsolidadoRow(
                m.id, m.nome, m.negocio, m.crescimentoFaturamentoPct,
                m.ferramentasConcluidas, m.ferramentasTotal,
                COUNT(e),
                SUM(CASE WHEN e.concluido = true THEN 1L ELSE 0L END),
                COALESCE(SUM(CAST(e.peso AS long)), 0L),
                COALESCE(SUM(CASE WHEN e.concluido = true THEN CAST(e.peso AS long) ELSE 0L END), 0L)
            )
            FROM Mentorado m LEFT JOIN Encaminhamento e ON e.mentorado = m
            GROUP BY m.id, m.nome, m.negocio, m.crescimentoFaturamentoPct, m.ferramentasConcluidas, m.ferramentasTotal
            """)
    List<MentoradoConsolidadoRow> buscarConsolidado();
}
