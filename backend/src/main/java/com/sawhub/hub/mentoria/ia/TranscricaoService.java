package com.sawhub.hub.mentoria.ia;

import java.nio.file.Path;

/** Porta pro provedor de transcrição (Whisper API, decisão do ROADMAP.md M06). Interface de
 * propósito — o pipeline assíncrono (AtaProcessamentoService) e os testes não conhecem o
 * provedor real, só este contrato. */
public interface TranscricaoService {
    String transcrever(Path arquivoAudio);
}
