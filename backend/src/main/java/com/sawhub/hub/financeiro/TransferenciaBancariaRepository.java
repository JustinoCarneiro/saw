package com.sawhub.hub.financeiro;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferenciaBancariaRepository extends JpaRepository<TransferenciaBancaria, UUID> {

    // open-in-view=false — mesmo raciocínio de PosicaoCaixaMensalRepository.buscarPorAnoMesComConta.
    @Query("SELECT t FROM TransferenciaBancaria t LEFT JOIN FETCH t.contaOrigem LEFT JOIN FETCH t.contaDestino "
            + "WHERE t.data BETWEEN :de AND :ate ORDER BY t.data DESC")
    List<TransferenciaBancaria> buscarPorPeriodo(@Param("de") LocalDate de, @Param("ate") LocalDate ate);
}
