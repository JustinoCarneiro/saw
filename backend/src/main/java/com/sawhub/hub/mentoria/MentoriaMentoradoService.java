package com.sawhub.hub.mentoria;

import com.sawhub.hub.conteudo.Conteudo;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.mentoria.dto.MentoriaMentoradoResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** H5.1-H5.3 (M12) — leitura mentee-facing sobre as mesmas entidades {@link Mentoria}/{@link Ata}
 * do Admin (M06), com isolamento por tenant e filtragem de visibilidade que o lado Admin não
 * precisa (ata RASCUNHO nunca aparece aqui). Ver Suposições do Blueprint M12 no ROADMAP.md. */
@Service
public class MentoriaMentoradoService {

    private final MentoriaRepository mentoriaRepository;
    private final AtaRepository ataRepository;
    private final MentoradoRepository mentoradoRepository;

    public MentoriaMentoradoService(MentoriaRepository mentoriaRepository, AtaRepository ataRepository,
                                     MentoradoRepository mentoradoRepository) {
        this.mentoriaRepository = mentoriaRepository;
        this.ataRepository = ataRepository;
        this.mentoradoRepository = mentoradoRepository;
    }

    public List<MentoriaMentoradoResponse> listar(UUID usuarioId) {
        Mentorado mentorado = resolverMentorado(usuarioId);
        List<Mentoria> mentorias = mentoriaRepository.buscarPorMentorado(mentorado);

        List<UUID> mentoriaIds = mentorias.stream().map(Mentoria::getId).toList();
        Map<UUID, Ata> atasPublicadasPorMentoria = ataRepository
                .findByMentoriaIdInAndStatus(mentoriaIds, StatusAta.PUBLICADA).stream()
                .collect(Collectors.toMap(a -> a.getMentoria().getId(), a -> a));

        Instant agora = Instant.now();
        return mentorias.stream()
                .map(m -> MentoriaMentoradoResponse.from(m, atasPublicadasPorMentoria.get(m.getId()),
                        materiaisVisiveis(m), agora))
                .toList();
    }

    // Reaproveita listar() em vez de uma busca dedicada: mesma checagem de posse (a mentoria só
    // aparece se :mentorado MEMBER OF m.mentorados, ver MentoriaRepository.buscarPorMentorado) sem
    // duplicar a regra em dois lugares. Escala pequena (10-15 mentorados, poucas mentorias cada,
    // ver CLAUDE.md § Princípios · Escala) — buscar tudo pra extrair um item não é desperdício
    // real aqui. NoSuchElementException tanto pra "não existe" quanto "não é seu" — mesmo padrão
    // anti-oráculo-de-enumeração do M11 (ConteudoMentoradoService).
    public byte[] gerarIcs(UUID usuarioId, UUID mentoriaId) {
        MentoriaMentoradoResponse mentoria = listar(usuarioId).stream()
                .filter(m -> m.id().equals(mentoriaId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Mentoria não encontrada."));

        Instant fim = mentoria.dataHora().plus(mentoria.duracaoMin(), ChronoUnit.MINUTES);
        String local = mentoria.linkOnline() != null ? mentoria.linkOnline() : mentoria.local();
        String summary = "Mentoria SAW HUB — " + mentoria.mentorNome();
        return IcsGenerator.gerar(mentoria.id(), summary, mentoria.dataHora(), fim, local, Instant.now());
    }

    private List<Conteudo> materiaisVisiveis(Mentoria mentoria) {
        return mentoria.getMateriaisRecomendados().stream()
                .filter(Conteudo::isPublicado)
                .toList();
    }

    private Mentorado resolverMentorado(UUID usuarioId) {
        return mentoradoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalStateException("Mentorado não encontrado para o usuário autenticado."));
    }
}
