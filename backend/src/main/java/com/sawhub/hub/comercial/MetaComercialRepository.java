package com.sawhub.hub.comercial;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MetaComercialRepository extends JpaRepository<MetaComercial, UUID> {

    Optional<MetaComercial> findByVendedorIdAndAnoAndMes(UUID vendedorId, Integer ano, Integer mes);

    // LEFT JOIN FETCH: `vendedor` é LAZY, RankingComercialService lê vendedor.nome fora da transação.
    @Query("SELECT m FROM MetaComercial m LEFT JOIN FETCH m.vendedor WHERE m.ano = :ano AND m.mes = :mes")
    List<MetaComercial> buscarComVendedorPorPeriodo(@Param("ano") Integer ano, @Param("mes") Integer mes);
}
