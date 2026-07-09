package com.sawhub.hub.mentoria;

import com.sawhub.hub.mentorado.Mentorado;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MentoriaRepository extends JpaRepository<Mentoria, UUID> {

    // DISTINCT + LEFT JOIN FETCH em mentor e mentorados: os dois são LAZY e o MentoriaResponse
    // (fora da transação, open-in-view=false) lê os dois — mesmo achado do LazyInitializationException
    // corrigido no LeadRepository (M05), aplicado aqui de propósito antes de virar bug em produção.
    // Bug ao vivo: um filtro opcional de Instant nulo em JPQL ("? IS NULL OR ...") faz o Postgres
    // tentar inferir o tipo do parâmetro e falhar ("could not determine data type" / "cannot cast
    // bytea to timestamp"), mesmo com CAST explícito nas duas ocorrências — problema de como o
    // Hibernate 6 vincula Instant dentro de uma função JPQL, não algo resolvível só ajustando a
    // query. O filtro de data/hora (de/ate) é feito em MentoriaService, em memória — mesmo padrão
    // já usado em LancamentoService (financeiro): dataset pequeno, endpoint admin autenticado,
    // sem o volume que justificaria complicar a query SQL por isso.
    @Query("SELECT DISTINCT m FROM Mentoria m LEFT JOIN FETCH m.mentor LEFT JOIN FETCH m.mentorados "
            + "WHERE (:status IS NULL OR m.status = :status) "
            + "ORDER BY m.dataHora ASC")
    List<Mentoria> buscarPorStatus(@Param("status") StatusMentoria status);

    @Query("SELECT DISTINCT m FROM Mentoria m LEFT JOIN FETCH m.mentor LEFT JOIN FETCH m.mentorados WHERE m.id = :id")
    Optional<Mentoria> buscarPorIdComDetalhes(@Param("id") UUID id);

    // M08 — Dashboard do Mentorado (H2.2). MEMBER OF exige o tipo do elemento da coleção
    // (Mentorado), não um UUID cru — o service já tem a entidade carregada, então passa ela.
    // Status/data futura são filtrados em MentoradoDashboardService, mesmo padrão do resto do
    // pacote (dataset pequeno por mentorado, sem o problema de inferência de tipo do Postgres).
    @Query("SELECT DISTINCT m FROM Mentoria m LEFT JOIN FETCH m.mentor LEFT JOIN FETCH m.mentorados "
            + "WHERE :mentorado MEMBER OF m.mentorados "
            + "ORDER BY m.dataHora ASC")
    List<Mentoria> buscarPorMentorado(@Param("mentorado") Mentorado mentorado);
}
