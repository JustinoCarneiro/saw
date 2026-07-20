package com.sawhub.hub.mentoria;

import com.sawhub.hub.atividade.AtividadeLogService;
import com.sawhub.hub.conteudo.Conteudo;
import com.sawhub.hub.conteudo.ConteudoRepository;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.mentoria.dto.CriarMentoriaRequest;
import com.sawhub.hub.mentoria.dto.RegistrarPresencasRequest;
import com.sawhub.hub.team.Colaborador;
import com.sawhub.hub.team.ColaboradorRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** H11.2 — criação e agenda de mentorias (individual/grupo). */
@Service
public class MentoriaService {

    private final MentoriaRepository mentoriaRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final MentoradoRepository mentoradoRepository;
    private final ConteudoRepository conteudoRepository;
    private final AtividadeLogService atividadeLogService;
    private final PresencaMentoriaRepository presencaMentoriaRepository;

    public MentoriaService(MentoriaRepository mentoriaRepository, ColaboradorRepository colaboradorRepository,
                            MentoradoRepository mentoradoRepository, ConteudoRepository conteudoRepository,
                            AtividadeLogService atividadeLogService, PresencaMentoriaRepository presencaMentoriaRepository) {
        this.mentoriaRepository = mentoriaRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.mentoradoRepository = mentoradoRepository;
        this.conteudoRepository = conteudoRepository;
        this.atividadeLogService = atividadeLogService;
        this.presencaMentoriaRepository = presencaMentoriaRepository;
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
            case CANCELADA -> {
                mentoria.cancelar();
                atividadeLogService.registrar("MENTORIA_CANCELADA", "Mentoria cancelada: " + nomesMentorados(mentoria));
            }
            // REALIZADA é tratado em AtaService.realizarMentoria — precisa criar a Ata
            // atomicamente com a transição, então não faz sentido aqui sem essa dependência.
            case REALIZADA -> throw new IllegalArgumentException(
                    "Use POST /mentorias/{id}/realizar (cria a ata automaticamente).");
            case AGENDADA -> throw new IllegalArgumentException("Não é possível mover uma mentoria de volta para Agendada.");
        }
        return mentoriaRepository.save(mentoria);
    }

    public Mentoria buscar(UUID id) {
        return mentoriaRepository.buscarPorIdComDetalhes(id)
                .orElseThrow(() -> new IllegalArgumentException("Mentoria não encontrada."));
    }

    static String nomesMentorados(Mentoria mentoria) {
        return mentoria.getMentorados().stream().map(Mentorado::getNome).collect(Collectors.joining(", "));
    }

    // M12 — pré-requisito de H5.2: sem isto, materiaisRecomendados nunca teria o que mostrar pro
    // mentorado (ver Suposições do Blueprint M12 no ROADMAP.md). IllegalArgumentException (400)
    // pra manter o mesmo padrão dos métodos irmãos desta classe (criar/buscar acima) — não o
    // NoSuchElementException (404) usado do lado mentee-facing (MentoriaMentoradoService): são
    // convenções diferentes por design, ver nota no ROADMAP.md M12.
    @Transactional
    public Mentoria atualizarMateriais(UUID id, List<UUID> conteudoIds) {
        Mentoria mentoria = buscar(id);
        List<Conteudo> conteudos = conteudoRepository.findAllById(conteudoIds);
        if (conteudos.size() != conteudoIds.size()) {
            throw new IllegalArgumentException("Um ou mais conteúdos não foram encontrados.");
        }
        mentoria.atualizarMateriaisRecomendados(new HashSet<>(conteudos));
        return mentoriaRepository.save(mentoria);
    }

    /** E17/M27 — presença por mentorado, só faz sentido pra mentoria GRUPO (individual já é
     * coberta pelo status da sessão inteira). Upsert por (mentoria, mentorado): chamar de novo
     * com o mesmo mentorado só atualiza o registro existente, não duplica. */
    @Transactional
    public Mentoria registrarPresencas(UUID id, RegistrarPresencasRequest request) {
        Mentoria mentoria = buscar(id);
        if (mentoria.getTipo() != TipoMentoria.GRUPO) {
            throw new IllegalArgumentException("Presença só se aplica a mentoria em grupo.");
        }
        for (RegistrarPresencasRequest.PresencaRequest p : request.presencas()) {
            Mentorado mentorado = mentoria.getMentorados().stream()
                    .filter(m -> m.getId().equals(p.mentoradoId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Mentorado " + p.mentoradoId() + " não participa desta mentoria."));
            var existente = presencaMentoriaRepository.findByMentoriaIdAndMentoradoId(id, p.mentoradoId());
            if (existente.isPresent()) {
                existente.get().marcar(p.presente());
                presencaMentoriaRepository.save(existente.get());
            } else {
                presencaMentoriaRepository.save(new PresencaMentoria(mentoria, mentorado, p.presente()));
            }
        }
        return mentoria;
    }
}
