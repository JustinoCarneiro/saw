package com.sawhub.hub.financeiro.dto;

import com.sawhub.hub.evento.Evento;
import java.time.Instant;
import java.util.UUID;

// Change request 17/07/2026 ("evento no financeiro") — só leitura, pro seletor de evento ao
// criar/filtrar conta a pagar/receber. Mesmo raciocínio de ComercialController.eventosParaVenda()
// (M25) e EventoVendaResumo: o EventoController "geral" é gated por Modulo.CONTEUDOS, então uma
// área Financeiro não-Fundador não conseguiria listar eventos por ali. Aqui não filtra por status
// (diferente do Comercial) — faz sentido marcar despesa/receita de evento já REALIZADO, não só
// dos ainda abertos pra venda.
public record EventoResumoFinanceiro(UUID id, String titulo, Instant dataHora) {
    public static EventoResumoFinanceiro from(Evento e) {
        return new EventoResumoFinanceiro(e.getId(), e.getTitulo(), e.getDataHora());
    }
}
