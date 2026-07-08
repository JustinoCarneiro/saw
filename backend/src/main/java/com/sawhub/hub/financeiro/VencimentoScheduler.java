package com.sawhub.hub.financeiro;

import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** H14.4 — marca PENDENTE -&gt; VENCIDO toda conta cujo vencimento já passou. Roda 1x/dia; não
 * precisa de mais frequência que isso (o próprio CLAUDE.md já define @Scheduled como o mecanismo
 * de agendamento do projeto, sem fila assíncrona pesada). */
@Component
public class VencimentoScheduler {

    private static final Logger log = LoggerFactory.getLogger(VencimentoScheduler.class);

    private final ContaPagarReceberRepository contaRepository;

    public VencimentoScheduler(ContaPagarReceberRepository contaRepository) {
        this.contaRepository = contaRepository;
    }

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void marcarContasVencidas() {
        List<ContaPagarReceber> pendentesVencidas = contaRepository
                .findByStatusAndDataVencimentoBefore(StatusConta.PENDENTE, LocalDate.now());
        pendentesVencidas.forEach(ContaPagarReceber::marcarVencida);
        contaRepository.saveAll(pendentesVencidas);
        if (!pendentesVencidas.isEmpty()) {
            log.info("{} conta(s) marcada(s) como VENCIDA.", pendentesVencidas.size());
        }
    }
}
