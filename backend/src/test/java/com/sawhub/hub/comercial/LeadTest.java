package com.sawhub.hub.comercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sawhub.hub.mentorado.TipoContrato;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** RED primeiro: Lead.criarJaFechado ainda não existe neste ponto do ciclo. M23 (change request
 * pós-MVP, 17/07/2026) — "criar mentorado direto" pula o funil (Solicitação->Em contato->
 * Proposta) e nasce direto em FECHADO, sem violar a máquina de estado (é um construtor
 * alternativo, não uma transição que burla exigirStatus()). */
class LeadTest {

    @Test
    void criarJaFechadoNasceEmStatusFechado() {
        Lead lead = Lead.criarJaFechado("Maria Souza", "maria@restaurante.com", "11999998888",
                TipoContrato.MENTORIA_CONTINUA);

        assertThat(lead.getStatus()).isEqualTo(StatusLead.FECHADO);
        assertThat(lead.getNome()).isEqualTo("Maria Souza");
        assertThat(lead.getEmail()).isEqualTo("maria@restaurante.com");
        assertThat(lead.getTelefone()).isEqualTo("11999998888");
        assertThat(lead.getTipoContratoFechado()).isEqualTo(TipoContrato.MENTORIA_CONTINUA);
        assertThat(lead.getDataFechamento()).isNotNull();
    }

    @Test
    void criarJaFechadoPermiteVincularMentoradoDepois() {
        Lead lead = Lead.criarJaFechado("Maria Souza", "maria@restaurante.com", null,
                TipoContrato.CONSULTORIA);

        // vincularMentorado exige FECHADO (exigirStatus) — não deve lançar.
        lead.vincularMentorado(null);
    }

    // M25 (change request pós-MVP, 17/07/2026) — DIAGNOSTICO é aditivo/opcional: bate com o
    // funil real (fluxograma_aline_comercial.pdf), mas não quebra o caminho direto
    // EM_CONTATO->PROPOSTA que o funil já suportava (mesma decisão de não misturar mudança de
    // schema com mudança de comportamento existente).
    @Test
    void moverParaDiagnosticoSoAPartirDeEmContato() {
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        lead.moverParaEmContato(new com.sawhub.hub.team.Colaborador(null, "Paula", com.sawhub.hub.team.Area.COMERCIAL));

        lead.moverParaDiagnostico();

        assertThat(lead.getStatus()).isEqualTo(StatusLead.DIAGNOSTICO);
    }

    @Test
    void moverParaDiagnosticoDeStatusErradoLancaErro() {
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);

        assertThatThrownBy(lead::moverParaDiagnostico).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void moverParaPropostaFuncionaTantoDeEmContatoQuantoDeDiagnostico() {
        Lead direto = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        direto.moverParaEmContato(new com.sawhub.hub.team.Colaborador(null, "Paula", com.sawhub.hub.team.Area.COMERCIAL));
        direto.moverParaProposta();
        assertThat(direto.getStatus()).isEqualTo(StatusLead.PROPOSTA);

        Lead comDiagnostico = new Lead("João Souza", "joao@restaurante.com", null, null, null);
        comDiagnostico.moverParaEmContato(new com.sawhub.hub.team.Colaborador(null, "Paula", com.sawhub.hub.team.Area.COMERCIAL));
        comDiagnostico.moverParaDiagnostico();
        comDiagnostico.moverParaProposta();
        assertThat(comDiagnostico.getStatus()).isEqualTo(StatusLead.PROPOSTA);
    }

    // M25 — "formulário único de venda": produto/origem/valor/forma de pagamento, aditivo em
    // paralelo a planoFechado/tipoContratoFechado (M23), mesma cisão sem substituir o legado.
    @Test
    void fecharVendaSoAPartirDePropostaEGravaTudo() {
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        lead.moverParaEmContato(new com.sawhub.hub.team.Colaborador(null, "Paula", com.sawhub.hub.team.Area.COMERCIAL));
        lead.moverParaProposta();

        lead.fecharVenda(ProdutoVenda.MENTORIA_CONTINUA, OrigemVenda.DIRETA,
                new BigDecimal("26000.00"), new BigDecimal("6000.00"), FormaPagamento.PIX);

        assertThat(lead.getStatus()).isEqualTo(StatusLead.FECHADO);
        assertThat(lead.getProdutoVenda()).isEqualTo(ProdutoVenda.MENTORIA_CONTINUA);
        assertThat(lead.getOrigemVenda()).isEqualTo(OrigemVenda.DIRETA);
        assertThat(lead.getValorTotalVenda()).isEqualByComparingTo("26000.00");
        assertThat(lead.getValorPagoNoAto()).isEqualByComparingTo("6000.00");
        assertThat(lead.getFormaPagamento()).isEqualTo(FormaPagamento.PIX);
        assertThat(lead.getDataFechamento()).isNotNull();
    }

    @Test
    void fecharVendaDeStatusErradoLancaErro() {
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);

        assertThatThrownBy(() -> lead.fecharVenda(ProdutoVenda.CONSULTORIA, OrigemVenda.DIRETA,
                BigDecimal.TEN, BigDecimal.ONE, FormaPagamento.PIX))
                .isInstanceOf(IllegalStateException.class);
    }

    // Gap 7 (raio-x + pesquisa da taxa real da Hotmart, confirmado 19/07/2026) — taxa de
    // plataforma retida é um terceiro conceito, distinto de valorPagoNoAto.
    @Test
    void fecharVendaComTaxaPlataformaRetidaGravaOTerceiroConceito() {
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        lead.moverParaEmContato(new com.sawhub.hub.team.Colaborador(null, "Paula", com.sawhub.hub.team.Area.COMERCIAL));
        lead.moverParaProposta();

        lead.fecharVenda(ProdutoVenda.PRODUTO_DIGITAL, OrigemVenda.HOTMART,
                new BigDecimal("1000.00"), new BigDecimal("890.00"), FormaPagamento.HOTMART,
                new BigDecimal("110.00"));

        assertThat(lead.getValorTotalVenda()).isEqualByComparingTo("1000.00");
        assertThat(lead.getValorPagoNoAto()).isEqualByComparingTo("890.00");
        assertThat(lead.getTaxaPlataformaRetida()).isEqualByComparingTo("110.00");
    }

    @Test
    void fecharVendaSemTaxaPlataformaRetidaFicaNula() {
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        lead.moverParaEmContato(new com.sawhub.hub.team.Colaborador(null, "Paula", com.sawhub.hub.team.Area.COMERCIAL));
        lead.moverParaProposta();

        lead.fecharVenda(ProdutoVenda.MENTORIA_CONTINUA, OrigemVenda.DIRETA,
                new BigDecimal("26000.00"), new BigDecimal("6000.00"), FormaPagamento.PIX);

        assertThat(lead.getTaxaPlataformaRetida()).isNull();
    }
}
