package com.sawhub.hub.mentoria.ia;

/** Lançada quando o provedor de IA (Whisper/Claude) falha ou não está configurado — capturada
 * pelo AtaProcessamentoService, que marca a ata como FALHA em vez de deixar a exceção estourar
 * numa thread assíncrona sem ninguém pra tratar. */
public class IaIndisponivelException extends RuntimeException {
    public IaIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }

    public IaIndisponivelException(String message) {
        super(message);
    }
}
