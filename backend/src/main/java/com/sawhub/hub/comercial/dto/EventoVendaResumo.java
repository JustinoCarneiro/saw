package com.sawhub.hub.comercial.dto;

import com.sawhub.hub.evento.Evento;
import java.time.Instant;
import java.util.UUID;

// M25 — só leitura, pro seletor de evento na venda de ingresso. Mesmo raciocínio de
// ComercialController.vendedores(): EventoController é gated por Modulo.CONTEUDOS, então uma
// área Comercial não-Fundador não conseguiria listar eventos por ali. Escopo mínimo: eventos
// ainda abertos pra venda (PROGRAMADO/AO_VIVO), sem os campos de curadoria de conteúdo.
public record EventoVendaResumo(UUID id, String titulo, Instant dataHora, Integer vagasDisponiveis) {
    public static EventoVendaResumo from(Evento e) {
        return new EventoVendaResumo(e.getId(), e.getTitulo(), e.getDataHora(), e.getVagasDisponiveis());
    }
}
