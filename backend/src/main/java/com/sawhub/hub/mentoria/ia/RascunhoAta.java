package com.sawhub.hub.mentoria.ia;

import java.util.List;

public record RascunhoAta(String resumo, String decisoes, List<EncaminhamentoSugerido> encaminhamentos) {
    public record EncaminhamentoSugerido(String titulo, int peso) {
    }
}
