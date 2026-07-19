package com.sawhub.hub.comercial.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sawhub.hub.comercial.FormaPagamento;
import com.sawhub.hub.comercial.OrigemVenda;
import com.sawhub.hub.comercial.ProdutoVenda;
import org.junit.jupiter.api.Test;

/** Gap 7 (achado do revisor-seguranca, 19/07/2026) — FecharVendaRequest é o primeiro record do
 * projeto com dois construtores (canônico de 9 args + secundário de 8, pra manter chamador Java
 * antigo compilando sem mudar nada). Prova que isso não quebra a desserialização JSON real:
 * Jackson sempre dirige pelo construtor canônico via reflection nativa de record (desde 2.12),
 * nunca pelo secundário — esse é só açúcar sintático pra chamada Java direta (testes), não afeta
 * o contrato HTTP em nenhum sentido. */
class FecharVendaRequestJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void desserializaComTaxaPlataformaRetidaPresente() throws Exception {
        String json = """
                {
                  "produtoVenda": "PRODUTO_DIGITAL",
                  "origemVenda": "HOTMART",
                  "valorTotalVenda": 1000.00,
                  "valorPagoNoAto": 890.00,
                  "formaPagamento": "HOTMART",
                  "taxaPlataformaRetida": 110.00
                }
                """;

        FecharVendaRequest request = mapper.readValue(json, FecharVendaRequest.class);

        assertThat(request.produtoVenda()).isEqualTo(ProdutoVenda.PRODUTO_DIGITAL);
        assertThat(request.origemVenda()).isEqualTo(OrigemVenda.HOTMART);
        assertThat(request.valorTotalVenda()).isEqualByComparingTo("1000.00");
        assertThat(request.valorPagoNoAto()).isEqualByComparingTo("890.00");
        assertThat(request.formaPagamento()).isEqualTo(FormaPagamento.HOTMART);
        assertThat(request.taxaPlataformaRetida()).isEqualByComparingTo("110.00");
    }

    // Chamador antigo (frontend anterior a esta leva, ou qualquer request sem o campo novo) —
    // Jackson trata chave ausente como null pro parâmetro correspondente do construtor canônico,
    // mesmo comportamento de parcelas/eventoId/ingressos (já nullable antes desta leva).
    @Test
    void desserializaSemTaxaPlataformaRetidaFicaNula() throws Exception {
        String json = """
                {
                  "produtoVenda": "CONSULTORIA",
                  "origemVenda": "DIRETA",
                  "valorTotalVenda": 9000.00,
                  "valorPagoNoAto": 9000.00,
                  "formaPagamento": "PIX"
                }
                """;

        FecharVendaRequest request = mapper.readValue(json, FecharVendaRequest.class);

        assertThat(request.taxaPlataformaRetida()).isNull();
        assertThat(request.valorTotalVenda()).isEqualByComparingTo("9000.00");
    }
}
