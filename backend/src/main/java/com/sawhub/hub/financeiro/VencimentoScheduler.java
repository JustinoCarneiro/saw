package com.sawhub.hub.financeiro;

import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** H14.4 — marca PREVISTO -&gt; VENCIDO todo lançamento com vencimento cujo prazo já passou. Roda
 * 1x/dia; não precisa de mais frequência que isso (o próprio CLAUDE.md já define @Scheduled como
 * o mecanismo de agendamento do projeto, sem fila assíncrona pesada). M26 — repontado de
 * {@code ContaPagarReceberRepository} pra {@code LancamentoFinanceiroRepository} (merge de
 * entidade, ver ROADMAP.md § "Blueprint (M26)"). */
@Component
public class VencimentoScheduler {

    private static final Logger log = LoggerFactory.getLogger(VencimentoScheduler.class);

    private final LancamentoFinanceiroRepository lancamentoRepository;

    public VencimentoScheduler(LancamentoFinanceiroRepository lancamentoRepository) {
        this.lancamentoRepository = lancamentoRepository;
    }

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void marcarContasVencidas() {
        List<LancamentoFinanceiro> previstosVencidos = lancamentoRepository
                .findByStatusAndDataVencimentoBefore(StatusLancamento.PREVISTO, LocalDate.now());
        previstosVencidos.forEach(LancamentoFinanceiro::marcarVencida);
        lancamentoRepository.saveAll(previstosVencidos);
        if (!previstosVencidos.isEmpty()) {
            log.info("{} lançamento(s) marcado(s) como VENCIDO.", previstosVencidos.size());
        }
    }
}
