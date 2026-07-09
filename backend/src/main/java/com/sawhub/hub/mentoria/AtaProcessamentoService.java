package com.sawhub.hub.mentoria;

import com.sawhub.hub.mentoria.ia.AtaRascunhoService;
import com.sawhub.hub.mentoria.ia.RascunhoAta;
import com.sawhub.hub.mentoria.ia.TranscricaoService;
import java.nio.file.Path;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Worker assíncrono do diferencial de IA (ROADMAP.md M06): transcrição (Whisper) -&gt; resumo
 * estruturado (Claude Sonnet 5) -&gt; persiste rascunho. Roda numa thread separada da requisição
 * HTTP que disparou o upload — por isso métodos próprios com {@code @Transactional} (a transação
 * do request original já fechou) e captura ampla de exceção (nada aqui pode propagar pra fora de
 * uma thread do pool sem tratamento — viraria falha silenciosa, sem ninguém pra devolver erro
 * pro usuário). */
@Service
public class AtaProcessamentoService {

    private static final Logger log = LoggerFactory.getLogger(AtaProcessamentoService.class);

    private final AtaRepository ataRepository;
    private final AtaEncaminhamentoSugeridoRepository sugeridoRepository;
    private final TranscricaoService transcricaoService;
    private final AtaRascunhoService ataRascunhoService;
    private final AudioStorageService audioStorageService;

    public AtaProcessamentoService(AtaRepository ataRepository, AtaEncaminhamentoSugeridoRepository sugeridoRepository,
                                    TranscricaoService transcricaoService, AtaRascunhoService ataRascunhoService,
                                    AudioStorageService audioStorageService) {
        this.ataRepository = ataRepository;
        this.sugeridoRepository = sugeridoRepository;
        this.transcricaoService = transcricaoService;
        this.ataRascunhoService = ataRascunhoService;
        this.audioStorageService = audioStorageService;
    }

    @Async("ataProcessamentoExecutor")
    public void processar(UUID ataId, String audioUrl) {
        try {
            Path arquivo = audioStorageService.resolver(audioUrl);
            String transcricao = transcricaoService.transcrever(arquivo);
            RascunhoAta rascunho = ataRascunhoService.gerarRascunho(transcricao);
            concluir(ataId, transcricao, rascunho);
        } catch (Exception e) {
            log.error("Falha ao processar IA da ata {}", ataId, e);
            falhar(ataId, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    @Transactional
    void concluir(UUID ataId, String transcricao, RascunhoAta rascunho) {
        Ata ata = ataRepository.findById(ataId)
                .orElseThrow(() -> new IllegalArgumentException("Ata não encontrada."));
        ata.concluirProcessamento(transcricao, rascunho.resumo());
        ataRepository.save(ata);
        for (RascunhoAta.EncaminhamentoSugerido sugestao : rascunho.encaminhamentos()) {
            sugeridoRepository.save(new AtaEncaminhamentoSugerido(ata, sugestao.titulo(), sugestao.peso(), true));
        }
    }

    @Transactional
    void falhar(UUID ataId, String erro) {
        Ata ata = ataRepository.findById(ataId)
                .orElseThrow(() -> new IllegalArgumentException("Ata não encontrada."));
        ata.falharProcessamento(erro);
        ataRepository.save(ata);
    }
}
