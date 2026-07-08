package com.sawhub.hub.financeiro;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContaPagarReceberRepository extends JpaRepository<ContaPagarReceber, UUID> {

    List<ContaPagarReceber> findAllByOrderByDataVencimentoAsc();

    List<ContaPagarReceber> findByTipoOrderByDataVencimentoAsc(TipoConta tipo);

    List<ContaPagarReceber> findByStatusOrderByDataVencimentoAsc(StatusConta status);

    List<ContaPagarReceber> findByStatusAndDataVencimentoBefore(StatusConta status, LocalDate data);
}
