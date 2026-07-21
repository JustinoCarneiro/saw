package com.sawhub.hub.mentoria;

import com.sawhub.hub.atividade.AtividadeLogService;
import com.sawhub.hub.mentorado.Encaminhamento;
import com.sawhub.hub.mentorado.EncaminhamentoRepository;
import com.sawhub.hub.mentorado.Mentorado;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/** H5.2 + diferencial de IA — ata nasce vazia quando a mentoria é realizada, opcionalmente
 * processada por IA, sempre revisada por humano antes de publicar. */
@Service
public class AtaService {

    private final AtaRepository ataRepository;
    private final MentoriaRepository mentoriaRepository;
    private final AtaEncaminhamentoSugeridoRepository sugeridoRepository;
    private final EncaminhamentoRepository encaminhamentoRepository;
    private final AudioStorageService audioStorageService;
    private final AtaProcessamentoService ataProcessamentoService;
    private final AtividadeLogService atividadeLogService;

    public AtaService(AtaRepository ataRepository, MentoriaRepository mentoriaRepository,
                       AtaEncaminhamentoSugeridoRepository sugeridoRepository,
                       EncaminhamentoRepository encaminhamentoRepository, AudioStorageService audioStorageService,
                       AtaProcessamentoService ataProcessamentoService, AtividadeLogService atividadeLogService) {
        this.ataRepository = ataRepository;
        this.mentoriaRepository = mentoriaRepository;
        this.sugeridoRepository = sugeridoRepository;
        this.encaminhamentoRepository = encaminhamentoRepository;
        this.audioStorageService = audioStorageService;
        this.ataProcessamentoService = ataProcessamentoService;
        this.atividadeLogService = atividadeLogService;
    }

    /** Transição REALIZADA da mentoria + criação da ata (vazia) numa operação só — CLAUDE.md:
     * "Realizada (gera ata)". Não cabe em MentoriaService.avancarStatus porque essa dependência
     * cruzada (Mentoria -> Ata) só existe aqui. */
    @Transactional
    public Ata realizarMentoria(UUID mentoriaId) {
        Mentoria mentoria = mentoriaRepository.buscarPorIdComDetalhes(mentoriaId)
                .orElseThrow(() -> new IllegalArgumentException("Mentoria não encontrada."));
        mentoria.realizar();
        mentoriaRepository.save(mentoria);
        atividadeLogService.registrar("MENTORIA_REALIZADA", "Mentoria realizada: " + MentoriaService.nomesMentorados(mentoria));
        return ataRepository.save(new Ata(mentoria));
    }

    public Ata buscarPorMentoria(UUID mentoriaId) {
        return ataRepository.findByMentoriaId(mentoriaId)
                .orElseThrow(() -> new IllegalArgumentException("Ata não encontrada para esta mentoria."));
    }

    public List<AtaEncaminhamentoSugerido> listarSugestoes(UUID mentoriaId) {
        Ata ata = buscarPorMentoria(mentoriaId);
        return sugeridoRepository.findByAtaIdOrderByTituloAsc(ata.getId());
    }

    // Achado (E2E de M06 c/ stub de IA rápido o bastante pra expor a corrida): disparar o
    // @Async de dentro do @Transactional lançava a outra thread ANTES do commit desta transação
    // terminar — a thread do processamento podia tentar salvar a Ata (concluir/falhar) enquanto
    // o commit de iniciarProcessamento() ainda estava em voo, dando
    // ObjectOptimisticLockingFailureException. Com Whisper/Claude reais isso quase nunca
    // aparecia (latência de rede real cobria o commit), mas era uma corrida de verdade, não uma
    // coincidência do teste. registerSynchronization(afterCommit) garante a ordem certa sempre.
    @Transactional
    public Ata iniciarUpload(UUID mentoriaId, MultipartFile audio) {
        Ata ata = buscarPorMentoria(mentoriaId);
        String url = audioStorageService.salvar(ata.getId(), audio);
        ata.iniciarProcessamento(url);
        Ata salva = ataRepository.save(ata);
        UUID ataId = salva.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                ataProcessamentoService.processar(ataId, url);
            }
        });
        return salva;
    }

    // M28 (change request, 21/07/2026) — "colar transcrição do Google Meet": aditivo, não
    // substitui o upload de áudio (mentor escolhe qual usar). Pula WhisperTranscricaoService por
    // completo — o texto colado JÁ é a transcrição, só falta o resumo/decisões/sugestões da IA
    // (ClaudeAtaRascunhoService, via AtaRascunhoService). Mesmo padrão de afterCommit de
    // iniciarUpload (ver comentário acima): dispara o @Async só depois do commit desta transação,
    // pela mesma corrida ObjectOptimisticLockingFailureException já achada ali.
    @Transactional
    public Ata iniciarComTranscricaoColada(UUID mentoriaId, String transcricao) {
        Ata ata = buscarPorMentoria(mentoriaId);
        ata.iniciarProcessamento(null);
        Ata salva = ataRepository.save(ata);
        UUID ataId = salva.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                ataProcessamentoService.processarTranscricaoColada(ataId, transcricao);
            }
        });
        return salva;
    }

    @Transactional
    public Ata editarResumo(UUID mentoriaId, String resumo) {
        Ata ata = buscarPorMentoria(mentoriaId);
        ata.editarResumo(resumo);
        return ataRepository.save(ata);
    }

    @Transactional
    public Ata editarDecisoes(UUID mentoriaId, String decisoes) {
        Ata ata = buscarPorMentoria(mentoriaId);
        ata.editarDecisoes(decisoes);
        return ataRepository.save(ata);
    }

    @Transactional
    public AtaEncaminhamentoSugerido editarSugestao(UUID mentoriaId, UUID sugestaoId, String titulo,
                                                     Integer pesoSugerido, boolean aceito) {
        Ata ata = buscarPorMentoria(mentoriaId);
        // Achado (baixo) da revisão de segurança do M06: sem isto, uma sugestão continuava editável
        // depois da ata PUBLICADA, divergindo do que já foi materializado em Encaminhamento — mesma
        // regra que editarResumo()/publicar() já aplicam via Ata.exigirRascunho().
        ata.exigirRascunho();
        AtaEncaminhamentoSugerido sugestao = sugeridoRepository.findById(sugestaoId)
                .filter(s -> s.getAta().getId().equals(ata.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Sugestão não encontrada para esta ata."));
        sugestao.editar(titulo, pesoSugerido, aceito);
        return sugeridoRepository.save(sugestao);
    }

    /** Publica a ata e materializa as sugestões aceitas em {@link Encaminhamento} de verdade —
     * só a partir daqui elas contam pro ranking do E17 (revisão humana obrigatória, ver
     * ROADMAP.md M06). Um encaminhamento por mentorado da mentoria: mesmo numa mentoria em grupo,
     * cada participante tem sua própria linha (a pontuação do E17 é por mentorado). */
    @Transactional
    public Ata publicar(UUID mentoriaId) {
        Mentoria mentoria = mentoriaRepository.buscarPorIdComDetalhes(mentoriaId)
                .orElseThrow(() -> new IllegalArgumentException("Mentoria não encontrada."));
        Ata ata = buscarPorMentoria(mentoriaId);
        ata.publicar();
        ataRepository.save(ata);

        for (AtaEncaminhamentoSugerido sugestao : sugeridoRepository.findByAtaIdOrderByTituloAsc(ata.getId())) {
            if (!sugestao.isAceito()) {
                continue;
            }
            for (Mentorado mentorado : mentoria.getMentorados()) {
                encaminhamentoRepository.save(new Encaminhamento(mentorado, sugestao.getTitulo(),
                        sugestao.getPesoSugerido(), false, mentoria));
            }
        }
        return ata;
    }
}
