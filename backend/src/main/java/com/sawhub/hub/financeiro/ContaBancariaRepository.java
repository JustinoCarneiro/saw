package com.sawhub.hub.financeiro;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContaBancariaRepository extends JpaRepository<ContaBancaria, UUID> {
    List<ContaBancaria> findByAtivaTrue();

    Optional<ContaBancaria> findByNomeIgnoreCase(String nome);
}
