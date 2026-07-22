package com.sawhub.hub.financeiro;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PosicaoCaixaMensalRepository extends JpaRepository<PosicaoCaixaMensal, UUID> {

    Optional<PosicaoCaixaMensal> findByContaBancariaIdAndAnoAndMes(UUID contaBancariaId, int ano, int mes);

    // open-in-view=false (mesmo raciocínio de VendaIngressoRepository.buscarPorEventoIdComLead):
    // contaBancaria é LAZY, sem o FETCH a serialização do Response fora da transação estoura
    // LazyInitializationException.
    @Query("SELECT p FROM PosicaoCaixaMensal p LEFT JOIN FETCH p.contaBancaria WHERE p.ano = :ano AND p.mes = :mes")
    List<PosicaoCaixaMensal> buscarPorAnoMesComConta(@Param("ano") int ano, @Param("mes") int mes);
}
