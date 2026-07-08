package com.sawhub.hub.financeiro;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaFinanceiraRepository extends JpaRepository<CategoriaFinanceira, UUID> {
}
