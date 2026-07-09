package com.sawhub.hub.mentorado;

import com.sawhub.hub.meta.Meta;
import com.sawhub.hub.meta.MetaRepository;
import com.sawhub.hub.mentorado.dto.AtualizarTarefaRequest;
import com.sawhub.hub.mentorado.dto.CriarTarefaRequest;
import com.sawhub.hub.mentorado.dto.ResumoTarefasResponse;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** H4.1–H4.4 (M10) — self-service do mentorado, mesmo padrão do M09 (Meta). Toda operação resolve
 * o {@link Mentorado} a partir do usuário autenticado; vínculo a {@link Meta} passa pela mesma
 * checagem de posse (404 genérico, não 403 — H1.1). Peso nunca vem de request: self-service nasce
 * sempre peso 1 (ver {@link Encaminhamento} construtor self-service). */
@Service
public class TarefaService {

    private final EncaminhamentoRepository encaminhamentoRepository;
    private final MentoradoRepository mentoradoRepository;
    private final MetaRepository metaRepository;

    public TarefaService(EncaminhamentoRepository encaminhamentoRepository, MentoradoRepository mentoradoRepository,
                          MetaRepository metaRepository) {
        this.encaminhamentoRepository = encaminhamentoRepository;
        this.mentoradoRepository = mentoradoRepository;
        this.metaRepository = metaRepository;
    }

    public Encaminhamento criar(UUID usuarioId, CriarTarefaRequest request) {
        Mentorado mentorado = mentoradoDoUsuario(usuarioId);
        Meta meta = resolverMetaDoUsuario(mentorado, request.metaId());
        Encaminhamento tarefa = new Encaminhamento(mentorado, request.titulo(), request.prazo(), request.prioridade(), meta);
        return encaminhamentoRepository.save(tarefa);
    }

    public List<Encaminhamento> listar(UUID usuarioId, StatusTarefa status, String busca) {
        Mentorado mentorado = mentoradoDoUsuario(usuarioId);
        return encaminhamentoRepository.buscarPorMentorado(mentorado.getId(), status, busca);
    }

    public ResumoTarefasResponse resumo(UUID usuarioId) {
        Mentorado mentorado = mentoradoDoUsuario(usuarioId);
        List<Encaminhamento> todas = encaminhamentoRepository.buscarPorMentorado(mentorado.getId(), null, null);
        long concluidas = todas.stream().filter(e -> e.getStatus() == StatusTarefa.CONCLUIDA).count();
        long emAndamento = todas.stream().filter(e -> e.getStatus() == StatusTarefa.EM_ANDAMENTO).count();
        long pendentes = todas.stream().filter(e -> e.getStatus() == StatusTarefa.PENDENTE).count();
        return new ResumoTarefasResponse(todas.size(), concluidas, emAndamento, pendentes);
    }

    public Encaminhamento atualizar(UUID usuarioId, UUID tarefaId, AtualizarTarefaRequest request) {
        Mentorado mentorado = mentoradoDoUsuario(usuarioId);
        Encaminhamento tarefa = buscarDoMentorado(mentorado, tarefaId);
        Meta meta = resolverMetaDoUsuario(mentorado, request.metaId());
        tarefa.editar(request.titulo(), request.prazo(), request.prioridade(), meta);
        // Retorna `tarefa` (não o valor de save()) de propósito — achado ao vivo: save() num
        // registro já persistido faz merge(), que devolve um objeto GERENCIADO NUM NOVO CONTEXTO
        // DE PERSISTÊNCIA, onde a associação `meta` volta a ser um proxy LAZY não inicializado
        // (mesmo com o FETCH JOIN de buscarDoMentorado). `tarefa` já tem a mutação aplicada em
        // memória E o meta original já carregado — sem esse risco.
        encaminhamentoRepository.save(tarefa);
        return tarefa;
    }

    public Encaminhamento avancarStatus(UUID usuarioId, UUID tarefaId, StatusTarefa novoStatus) {
        Mentorado mentorado = mentoradoDoUsuario(usuarioId);
        Encaminhamento tarefa = buscarDoMentorado(mentorado, tarefaId);
        switch (novoStatus) {
            case EM_ANDAMENTO -> tarefa.iniciar();
            case CONCLUIDA -> tarefa.concluir();
            case PENDENTE -> tarefa.reabrir();
        }
        // Mesmo motivo do atualizar() acima — não retornar o valor de save().
        encaminhamentoRepository.save(tarefa);
        return tarefa;
    }

    private Mentorado mentoradoDoUsuario(UUID usuarioId) {
        return mentoradoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalStateException(
                        "Usuário autenticado (id=" + usuarioId + ") não tem Mentorado vinculado."));
    }

    // 404 genérico pra "não existe" e "existe mas não é sua" (isolamento por tenant, H1.1).
    // buscarPorIdComMeta (não findById puro) — achado ao vivo: meta é LAZY e o TarefaResponse
    // (fora da transação) lê o título dela; ver nota em EncaminhamentoRepository.
    private Encaminhamento buscarDoMentorado(Mentorado mentorado, UUID tarefaId) {
        return encaminhamentoRepository.buscarPorIdComMeta(tarefaId)
                .filter(e -> e.getMentorado().getId().equals(mentorado.getId()))
                .orElseThrow(() -> new NoSuchElementException("Tarefa não encontrada."));
    }

    // Mesma checagem de posse — vincular a uma Meta de outro mentorado é rejeitado como se a
    // Meta não existisse, mesmo que a tarefa em si seja do próprio usuário autenticado.
    private Meta resolverMetaDoUsuario(Mentorado mentorado, UUID metaId) {
        if (metaId == null) {
            return null;
        }
        return metaRepository.findById(metaId)
                .filter(m -> m.getMentorado().getId().equals(mentorado.getId()))
                .orElseThrow(() -> new NoSuchElementException("Meta não encontrada."));
    }
}
