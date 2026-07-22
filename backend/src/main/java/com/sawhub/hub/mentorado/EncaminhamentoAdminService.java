package com.sawhub.hub.mentorado;

import com.sawhub.hub.mentorado.dto.AtualizarEncaminhamentoAdminRequest;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fase 5 (H4.6) — achado ao vivo (22/07/2026, pedido do Marcos): a tela Admin de Tarefas só tinha
 * listagem + import/export CSV, sem forma de editar título/prazo/prioridade nem avançar status
 * (Iniciar/Concluir/Reabrir) de um encaminhamento já existente — inclusive os materializados na
 * publicação de uma ata (M06), que nunca têm prazo de origem. Mesmo padrão de {@link TarefaService}
 * (self-service do mentorado), sem o escopo por usuário/posse: Admin edita qualquer encaminhamento
 * de qualquer mentorado. */
@Service
public class EncaminhamentoAdminService {

    private final EncaminhamentoRepository encaminhamentoRepository;

    public EncaminhamentoAdminService(EncaminhamentoRepository encaminhamentoRepository) {
        this.encaminhamentoRepository = encaminhamentoRepository;
    }

    @Transactional
    public Encaminhamento atualizar(UUID id, AtualizarEncaminhamentoAdminRequest request) {
        Encaminhamento tarefa = buscar(id);
        // Meta preservada como estava — vínculo a Meta é decisão de self-service do mentorado
        // (TarefaService), fora de escopo da edição Admin.
        tarefa.editar(request.titulo(), request.prazo(), request.prioridade(), tarefa.getMeta());
        encaminhamentoRepository.save(tarefa);
        return tarefa;
    }

    @Transactional
    public Encaminhamento avancarStatus(UUID id, StatusTarefa novoStatus) {
        Encaminhamento tarefa = buscar(id);
        switch (novoStatus) {
            case EM_ANDAMENTO -> tarefa.iniciar();
            case CONCLUIDA -> tarefa.concluir();
            case PENDENTE -> tarefa.reabrir();
        }
        encaminhamentoRepository.save(tarefa);
        return tarefa;
    }

    private Encaminhamento buscar(UUID id) {
        return encaminhamentoRepository.buscarPorIdComMentorado(id)
                .orElseThrow(() -> new NoSuchElementException("Encaminhamento não encontrado."));
    }
}
