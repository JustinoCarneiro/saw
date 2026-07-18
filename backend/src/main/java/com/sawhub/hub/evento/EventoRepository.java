package com.sawhub.hub.evento;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoRepository extends JpaRepository<Evento, UUID> {

    @Query("SELECT e FROM Evento e "
            + "WHERE (:tipo IS NULL OR e.tipo = :tipo) "
            + "AND (:status IS NULL OR e.status = :status) "
            + "ORDER BY e.dataHora ASC")
    List<Evento> buscarComFiltro(@Param("tipo") TipoEvento tipo, @Param("status") StatusEvento status);

    // M13 (H7.1) — spec.md diz "Dado eventos programados": só PROGRAMADO/AO_VIVO, diferente do
    // buscarComFiltro acima (admin-wide, qualquer status). Status como lista vinculada (não
    // literal de enum na query) — forma garantidamente suportada pelo Hibernate 6, mesmo padrão
    // do resto do projeto (sempre parâmetro, nunca literal hardcoded em JPQL). Filtro de tema em
    // memória no service — mesmo padrão de dataset pequeno já usado no resto do pacote (de/ate em
    // MentoriaService).
    @Query("SELECT e FROM Evento e "
            + "WHERE e.status IN :statuses "
            + "AND (:tipo IS NULL OR e.tipo = :tipo) "
            + "ORDER BY e.dataHora ASC")
    List<Evento> buscarPorStatusIn(@Param("statuses") List<StatusEvento> statuses, @Param("tipo") TipoEvento tipo);

    /** M25 — dashboard comercial (Suposição 7): venda de ingresso é contabilizada pela data em
     * que o Evento acontece, não pela data da venda. Não existe um campo "dataRealizacao"
     * separado (finalizar() só muda status) — dataHora (agendada) é a melhor aproximação
     * disponível hoje pra "em que mês esse evento caiu"; simplificação aceita, documentada. */
    @Query("SELECT e FROM Evento e WHERE e.status = :status AND e.dataHora BETWEEN :de AND :ate")
    List<Evento> buscarPorStatusEDataHoraBetween(@Param("status") StatusEvento status,
            @Param("de") Instant de, @Param("ate") Instant ate);
}
