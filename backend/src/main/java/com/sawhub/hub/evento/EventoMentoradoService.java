package com.sawhub.hub.evento;

import com.sawhub.hub.evento.dto.EventoMentoradoResponse;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** H7.1-H7.2 (M13) — leitura + inscrição mentee-facing sobre a mesma entidade {@link Evento} do
 * Admin (M06). Mesmo padrão de isolamento do M08-M12: o id do Mentorado nunca vem de parâmetro,
 * só do usuário autenticado. */
@Service
public class EventoMentoradoService {

    private final EventoRepository eventoRepository;
    private final InscricaoEventoRepository inscricaoEventoRepository;
    private final MentoradoRepository mentoradoRepository;

    public EventoMentoradoService(EventoRepository eventoRepository, InscricaoEventoRepository inscricaoEventoRepository,
                                   MentoradoRepository mentoradoRepository) {
        this.eventoRepository = eventoRepository;
        this.inscricaoEventoRepository = inscricaoEventoRepository;
        this.mentoradoRepository = mentoradoRepository;
    }

    // spec.md H7.1: só eventos PROGRAMADO/AO_VIVO (ver EventoRepository.buscarPorStatusIn) —
    // diferente do M12, este épico não pede histórico de eventos passados.
    public List<EventoMentoradoResponse> listar(UUID usuarioId, TipoEvento tipo, String tema) {
        Mentorado mentorado = resolverMentorado(usuarioId);
        List<Evento> eventos = eventoRepository.buscarPorStatusIn(
                List.of(StatusEvento.PROGRAMADO, StatusEvento.AO_VIVO), tipo);
        if (tema != null && !tema.isBlank()) {
            eventos = eventos.stream()
                    .filter(e -> e.getTema() != null && e.getTema().toLowerCase().contains(tema.toLowerCase()))
                    .toList();
        }

        Set<UUID> inscritos = inscricaoEventoRepository.findByMentoradoId(mentorado.getId()).stream()
                .filter(i -> i.getStatus() != StatusInscricao.CANCELADA)
                .map(i -> i.getEvento().getId())
                .collect(Collectors.toSet());

        return eventos.stream().map(e -> EventoMentoradoResponse.from(e, inscritos.contains(e.getId()))).toList();
    }

    @Transactional
    public EventoMentoradoResponse inscrever(UUID usuarioId, UUID eventoId) {
        Mentorado mentorado = resolverMentorado(usuarioId);
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new NoSuchElementException("Evento não encontrado."));
        if (evento.getStatus() != StatusEvento.PROGRAMADO && evento.getStatus() != StatusEvento.AO_VIVO) {
            throw new IllegalStateException("Este evento não está aceitando inscrições.");
        }

        InscricaoEvento inscricao = inscricaoEventoRepository
                .findByMentoradoIdAndEventoId(mentorado.getId(), eventoId).orElse(null);
        // Idempotente: reinscrever num evento em que já está INSCRITA não é erro, nem ocupa uma
        // segunda vaga (duplo clique/retry de rede não deve punir o mentorado).
        if (inscricao != null && inscricao.getStatus() == StatusInscricao.INSCRITA) {
            return EventoMentoradoResponse.from(evento, true);
        }

        // ocuparVaga() muta Evento (protegido por @Version) na mesma transação que salva a
        // InscricaoEvento — duas inscrições concorrentes na última vaga fazem a 2ª save() do
        // Evento estourar 409 (ObjectOptimisticLockingFailureException), não silenciosamente
        // ultrapassar a capacidade. Ver ROADMAP.md M13.
        evento.ocuparVaga();
        eventoRepository.save(evento);

        if (inscricao == null) {
            inscricao = new InscricaoEvento(mentorado, evento);
        } else {
            inscricao.reinscrever();
        }
        inscricaoEventoRepository.save(inscricao);

        return EventoMentoradoResponse.from(evento, true);
    }

    @Transactional
    public void cancelar(UUID usuarioId, UUID eventoId) {
        Mentorado mentorado = resolverMentorado(usuarioId);
        InscricaoEvento inscricao = inscricaoEventoRepository
                .findByMentoradoIdAndEventoId(mentorado.getId(), eventoId)
                .filter(i -> i.getStatus() == StatusInscricao.INSCRITA)
                .orElseThrow(() -> new NoSuchElementException("Inscrição não encontrada."));

        inscricao.cancelar();
        inscricaoEventoRepository.save(inscricao);

        Evento evento = inscricao.getEvento();
        evento.liberarVaga();
        eventoRepository.save(evento);
    }

    private Mentorado resolverMentorado(UUID usuarioId) {
        return mentoradoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalStateException("Mentorado não encontrado para o usuário autenticado."));
    }
}
