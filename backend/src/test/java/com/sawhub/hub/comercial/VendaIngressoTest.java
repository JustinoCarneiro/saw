package com.sawhub.hub.comercial;

import static org.assertj.core.api.Assertions.assertThat;

import com.sawhub.hub.evento.Evento;
import com.sawhub.hub.evento.TipoEvento;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** RED primeiro: VendaIngresso ainda não existe. M25 — uma linha por ingresso (não por venda),
 * credenciamento é nominal (mesmo padrão das planilhas reais "Vendas Eventos"/"CREDENCIAMENTO"). */
class VendaIngressoTest {

    @Test
    void nasceSemCheckInEDepoisPodeMarcar() {
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null);
        Evento evento = new Evento("Receita do Sucesso", TipoEvento.PRESENCIAL, "Gestão", Instant.now(), "Recife", null, 100);

        VendaIngresso venda = new VendaIngresso(lead, evento, CategoriaIngresso.VIP, "João Comprador", "Financeiro", true,
                "Restaurante do João Ltda", "81999998888", "joao@restaurante.com");

        assertThat(venda.isCheckIn()).isFalse();
        assertThat(venda.getCategoriaIngresso()).isEqualTo(CategoriaIngresso.VIP);
        assertThat(venda.getNomeCredenciado()).isEqualTo("João Comprador");
        assertThat(venda.isAlmoco()).isTrue();
        assertThat(venda.getNomeEmpresa()).isEqualTo("Restaurante do João Ltda");
        assertThat(venda.getTelefone()).isEqualTo("81999998888");
        assertThat(venda.getEmail()).isEqualTo("joao@restaurante.com");

        venda.marcarCheckIn();

        assertThat(venda.isCheckIn()).isTrue();
    }

    // Gap 3 (raio-x, 19/07/2026) — nem toda venda real tem empresa/telefone/e-mail preenchidos.
    @Test
    void nomeEmpresaTelefoneEEmailSaoOpcionais() {
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null);
        Evento evento = new Evento("Receita do Sucesso", TipoEvento.PRESENCIAL, "Gestão", Instant.now(), "Recife", null, 100);

        VendaIngresso venda = new VendaIngresso(lead, evento, CategoriaIngresso.ESSENCIAL, "Ana Sócia", null, false,
                null, null, null);

        assertThat(venda.getNomeEmpresa()).isNull();
        assertThat(venda.getTelefone()).isNull();
        assertThat(venda.getEmail()).isNull();
    }
}
