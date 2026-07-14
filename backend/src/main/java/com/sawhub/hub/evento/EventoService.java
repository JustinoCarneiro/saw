package com.sawhub.hub.evento;

import com.sawhub.hub.atividade.AtividadeLogService;
import com.sawhub.hub.evento.dto.AtualizarEventoRequest;
import com.sawhub.hub.evento.dto.CriarEventoRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** H11.4 — CRUD admin de eventos e transições da máquina de estado. */
@Service
public class EventoService {

    private final EventoRepository eventoRepository;
    private final InscricaoEventoRepository inscricaoEventoRepository;
    private final AtividadeLogService atividadeLogService;

    public EventoService(EventoRepository eventoRepository, InscricaoEventoRepository inscricaoEventoRepository,
                          AtividadeLogService atividadeLogService) {
        this.eventoRepository = eventoRepository;
        this.inscricaoEventoRepository = inscricaoEventoRepository;
        this.atividadeLogService = atividadeLogService;
    }

    @Transactional
    public Evento criar(CriarEventoRequest request) {
        Evento evento = new Evento(request.titulo(), request.tipo(), request.tema(), request.dataHora(),
                request.local(), request.linkOnline(), request.vagas());
        return eventoRepository.save(evento);
    }

    public List<Evento> listar(TipoEvento tipo, StatusEvento status) {
        return eventoRepository.buscarComFiltro(tipo, status);
    }

    @Transactional
    public Evento atualizar(UUID id, AtualizarEventoRequest request) {
        Evento evento = buscar(id);
        evento.atualizar(request.titulo(), request.tema(), request.dataHora(), request.local(),
                request.linkOnline(), request.vagas());
        return eventoRepository.save(evento);
    }

    @Transactional
    public Evento avancarStatus(UUID id, StatusEvento novoStatus) {
        Evento evento = buscar(id);
        switch (novoStatus) {
            case AO_VIVO -> evento.iniciar();
            case REALIZADO -> {
                evento.finalizar();
                marcarParticipacoes(evento);
            }
            case CANCELADO -> {
                evento.cancelar();
                atividadeLogService.registrar("EVENTO_CANCELADO", "Evento cancelado: " + evento.getTitulo());
            }
            case PROGRAMADO -> throw new IllegalArgumentException("Não é possível mover um evento de volta para Programado.");
        }
        return eventoRepository.save(evento);
    }

    // H7.2 (M13) — sem tela de check-in no Admin nesta leva: se o evento aconteceu e o mentorado
    // não cancelou a inscrição, participou. Ver javadoc de InscricaoEvento.
    private void marcarParticipacoes(Evento evento) {
        for (InscricaoEvento inscricao : inscricaoEventoRepository.findByEventoIdAndStatus(evento.getId(), StatusInscricao.INSCRITA)) {
            inscricao.marcarParticipacao();
            inscricaoEventoRepository.save(inscricao);
        }
    }

    private Evento buscar(UUID id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Evento não encontrado."));
    }
}
