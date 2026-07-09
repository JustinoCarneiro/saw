package com.sawhub.hub.evento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InscricaoEventoRepository extends JpaRepository<InscricaoEvento, InscricaoEventoId> {

    // M13 — EventoMentoradoService usa pra saber quais eventos o mentorado logado já está
    // inscrito (campo "inscrito" da resposta), sem precisar de N chamadas (uma por evento).
    List<InscricaoEvento> findByMentoradoId(UUID mentoradoId);

    Optional<InscricaoEvento> findByMentoradoIdAndEventoId(UUID mentoradoId, UUID eventoId);

    // EventoService.avancarStatus(REALIZADO) usa pra marcar em lote toda inscrição ainda INSCRITA
    // como PARTICIPOU — ver Evento/InscricaoEvento javadoc (H7.2, sem check-in manual nesta leva).
    List<InscricaoEvento> findByEventoIdAndStatus(UUID eventoId, StatusInscricao status);
}
