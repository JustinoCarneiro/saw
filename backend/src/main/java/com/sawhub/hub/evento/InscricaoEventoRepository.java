package com.sawhub.hub.evento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InscricaoEventoRepository extends JpaRepository<InscricaoEvento, InscricaoEventoId> {

    // M13 — EventoMentoradoService usa pra saber quais eventos o mentorado logado já está
    // inscrito (campo "inscrito" da resposta), sem precisar de N chamadas (uma por evento). Só
    // lê i.getEvento().getId() (não dispara lazy-load — o id já vem no proxy pela FK), por isso
    // não precisa de FETCH JOIN. Quem precisar de outros campos de Evento usa
    // buscarPorMentoradoComEvento abaixo.
    List<InscricaoEvento> findByMentoradoId(UUID mentoradoId);

    // M28 (change request, 21/07/2026) — achado ao vivo (LazyInitializationException, mesma
    // classe de bug já documentada em MentoriaRepository): EventoInscricaoAdminResponse e o
    // cálculo de cota (EventoMentoradoService) leem título/data/tipo do Evento fora da transação
    // (open-in-view=false) — precisa de FETCH JOIN, `evento` é @ManyToOne LAZY.
    @Query("SELECT i FROM InscricaoEvento i LEFT JOIN FETCH i.evento WHERE i.mentorado.id = :mentoradoId")
    List<InscricaoEvento> buscarPorMentoradoComEvento(@Param("mentoradoId") UUID mentoradoId);

    Optional<InscricaoEvento> findByMentoradoIdAndEventoId(UUID mentoradoId, UUID eventoId);

    // EventoService.avancarStatus(REALIZADO) usa pra marcar em lote toda inscrição ainda INSCRITA
    // como PARTICIPOU — ver Evento/InscricaoEvento javadoc (H7.2, sem check-in manual nesta leva).
    List<InscricaoEvento> findByEventoIdAndStatus(UUID eventoId, StatusInscricao status);
}
