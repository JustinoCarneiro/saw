package com.sawhub.hub.mentoria;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtaRepository extends JpaRepository<Ata, UUID> {
    Optional<Ata> findByMentoriaId(UUID mentoriaId);
}
