package com.sawhub.hub.atividade;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** H10 — chamado a partir dos pontos de transição de status que contam como "marco" pro feed de
 * atividades recentes (ver DashboardAdminService.atividadesRecentes). Propagação padrão
 * (REQUIRED): participa da mesma transação do método que chamou, então uma transição que sofre
 * rollback (ex.: exceção depois do registrar()) não deixa um log órfão de algo que não aconteceu. */
@Service
public class AtividadeLogService {

    private final AtividadeLogRepository atividadeLogRepository;

    public AtividadeLogService(AtividadeLogRepository atividadeLogRepository) {
        this.atividadeLogRepository = atividadeLogRepository;
    }

    @Transactional
    public void registrar(String tipo, String descricao) {
        atividadeLogRepository.save(new AtividadeLog(tipo, descricao));
    }

    public List<AtividadeLog> listarRecentes() {
        return atividadeLogRepository.findAllByOrderByCriadoEmDesc();
    }
}
