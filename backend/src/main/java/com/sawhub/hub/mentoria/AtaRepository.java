package com.sawhub.hub.mentoria;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtaRepository extends JpaRepository<Ata, UUID> {
    Optional<Ata> findByMentoriaId(UUID mentoriaId);

    // M12 — busca em lote pro mentee-facing MentoriaMentoradoService.listar(): uma query só pra
    // todas as mentorias do mentorado, em vez de N chamadas (uma por mentoria) num loop. Filtra
    // status = PUBLICADA aqui, não em memória depois — RASCUNHO nunca deve viajar pro service que
    // monta a resposta do mentorado (ver Suposições do Blueprint M12 no ROADMAP.md).
    List<Ata> findByMentoriaIdInAndStatus(Collection<UUID> mentoriaIds, StatusAta status);
}
