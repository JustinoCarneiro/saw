package com.sawhub.hub.mentoria;

import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.mentoria.dto.CriarMentoriaRequest;
import com.sawhub.hub.team.Colaborador;
import com.sawhub.hub.team.ColaboradorRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** H11.2 — criação e agenda de mentorias (individual/grupo). */
@Service
public class MentoriaService {

    private final MentoriaRepository mentoriaRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final MentoradoRepository mentoradoRepository;

    public MentoriaService(MentoriaRepository mentoriaRepository, ColaboradorRepository colaboradorRepository,
                            MentoradoRepository mentoradoRepository) {
        this.mentoriaRepository = mentoriaRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.mentoradoRepository = mentoradoRepository;
    }

    @Transactional
    public Mentoria criar(CriarMentoriaRequest request) {
        if (request.tipo() == TipoMentoria.INDIVIDUAL && request.mentoradoIds().size() != 1) {
            throw new IllegalArgumentException("Mentoria individual precisa de exatamente 1 mentorado.");
        }

        Colaborador mentor = colaboradorRepository.findById(request.mentorId())
                .orElseThrow(() -> new IllegalArgumentException("Mentor não encontrado."));

        Set<Mentorado> mentorados = new HashSet<>(mentoradoRepository.findAllById(request.mentoradoIds()));
        if (mentorados.size() != request.mentoradoIds().size()) {
            throw new IllegalArgumentException("Um ou mais mentorados não foram encontrados.");
        }

        Mentoria mentoria = new Mentoria(request.tipo(), mentor, mentorados, request.dataHora(),
                request.duracaoMin(), request.linkOnline(), request.local());
        return mentoriaRepository.save(mentoria);
    }

    public List<Mentoria> listar(StatusMentoria status, Instant de, Instant ate) {
        // Filtro de data em memória — ver nota em MentoriaRepository.buscarPorStatus.
        return mentoriaRepository.buscarPorStatus(status).stream()
                .filter(m -> de == null || !m.getDataHora().isBefore(de))
                .filter(m -> ate == null || !m.getDataHora().isAfter(ate))
                .toList();
    }

    @Transactional
    public Mentoria avancarStatus(UUID id, StatusMentoria novoStatus) {
        Mentoria mentoria = buscar(id);
        switch (novoStatus) {
            case CONFIRMADA -> mentoria.confirmar();
            case CANCELADA -> mentoria.cancelar();
            // REALIZADA é tratado em AtaService.realizarMentoria — precisa criar a Ata
            // atomicamente com a transição, então não faz sentido aqui sem essa dependência.
            case REALIZADA -> throw new IllegalArgumentException(
                    "Use POST /mentorias/{id}/realizar (cria a ata automaticamente).");
            case AGENDADA -> throw new IllegalArgumentException("Não é possível mover uma mentoria de volta para Agendada.");
        }
        return mentoriaRepository.save(mentoria);
    }

    protected Mentoria buscar(UUID id) {
        return mentoriaRepository.buscarPorIdComDetalhes(id)
                .orElseThrow(() -> new IllegalArgumentException("Mentoria não encontrada."));
    }
}
