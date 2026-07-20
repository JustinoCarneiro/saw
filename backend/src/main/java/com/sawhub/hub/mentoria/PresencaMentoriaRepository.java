package com.sawhub.hub.mentoria;

import com.sawhub.hub.mentoria.dto.FrequenciaMentoriaRow;
import com.sawhub.hub.mentoria.dto.PresencaResumoRow;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PresencaMentoriaRepository extends JpaRepository<PresencaMentoria, UUID> {

    Optional<PresencaMentoria> findByMentoriaIdAndMentoradoId(UUID mentoriaId, UUID mentoradoId);

    // Projeção escalar (ver Javadoc de PresencaResumoRow) — usada pra montar o mapa mentoradoId→
    // presente que MentoriaResponse.from() precisa, sem risco de LazyInitializationException.
    @Query("SELECT new com.sawhub.hub.mentoria.dto.PresencaResumoRow(p.mentorado.id, p.presente) "
            + "FROM PresencaMentoria p WHERE p.mentoria.id = :mentoriaId")
    List<PresencaResumoRow> buscarResumoPorMentoria(@Param("mentoriaId") UUID mentoriaId);

    // E17/M27 — "frequenciaMentoriaPct" do Painel Consolidado: pra cada mentorado, quantas
    // mentorias GRUPO/REALIZADA ele participou (mt.mentorados) vs. quantas teve presença
    // confirmada (LEFT JOIN — uma mentoria sem PresencaMentoria ainda conta no total, só não no
    // numerador). Não filtra por mentorado aqui de propósito: mesmo padrão de
    // ConsolidatedRepository.buscarConsolidado(), busca tudo de uma vez, uma linha por mentorado.
    @Query("""
            SELECT new com.sawhub.hub.mentoria.dto.FrequenciaMentoriaRow(
                mo.id,
                COUNT(DISTINCT mt.id),
                COUNT(DISTINCT CASE WHEN p.presente = true THEN p.id END)
            )
            FROM Mentoria mt JOIN mt.mentorados mo
            LEFT JOIN PresencaMentoria p ON p.mentoria = mt AND p.mentorado = mo
            WHERE mt.tipo = com.sawhub.hub.mentoria.TipoMentoria.GRUPO
            AND mt.status = com.sawhub.hub.mentoria.StatusMentoria.REALIZADA
            GROUP BY mo.id
            """)
    List<FrequenciaMentoriaRow> buscarFrequencia();
}
