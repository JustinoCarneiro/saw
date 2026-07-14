package com.sawhub.hub.loja;

import com.sawhub.hub.atividade.AtividadeLogService;
import com.sawhub.hub.financeiro.CategoriaFinanceira;
import com.sawhub.hub.financeiro.CategoriaFinanceiraRepository;
import com.sawhub.hub.financeiro.LancamentoFinanceiro;
import com.sawhub.hub.financeiro.LancamentoFinanceiroRepository;
import com.sawhub.hub.financeiro.OrigemReceita;
import com.sawhub.hub.financeiro.StatusLancamento;
import com.sawhub.hub.financeiro.TipoLancamento;
import com.sawhub.hub.loja.pagamento.MercadoPagoGatewayService;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** H8.3-H8.4 (M14) — confirma pagamento a partir de uma notificação JÁ VERIFICADA por assinatura
 * (ver WebhookMercadoPagoController). Nunca confia no corpo do webhook: sempre re-consulta o
 * Payment de verdade na API do Mercado Pago a partir só do id recebido (ver Suposições do
 * Blueprint M14). Idempotente — notificação duplicada do gateway (comum em integrações de
 * webhook) não reprocessa nem gera receita em duplicidade. */
@Service
public class PedidoPagamentoService {

    private static final String STATUS_APROVADO = "approved";

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final CategoriaFinanceiraRepository categoriaFinanceiraRepository;
    private final LancamentoFinanceiroRepository lancamentoFinanceiroRepository;
    private final MercadoPagoGatewayService gateway;
    private final AtividadeLogService atividadeLogService;

    public PedidoPagamentoService(PedidoRepository pedidoRepository, ProdutoRepository produtoRepository,
                                   CategoriaFinanceiraRepository categoriaFinanceiraRepository,
                                   LancamentoFinanceiroRepository lancamentoFinanceiroRepository,
                                   MercadoPagoGatewayService gateway, AtividadeLogService atividadeLogService) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
        this.categoriaFinanceiraRepository = categoriaFinanceiraRepository;
        this.lancamentoFinanceiroRepository = lancamentoFinanceiroRepository;
        this.gateway = gateway;
        this.atividadeLogService = atividadeLogService;
    }

    @Transactional
    public void processarNotificacao(String paymentId) {
        var pagamento = gateway.consultarPagamento(paymentId);
        if (pagamento.pedidoId() == null) {
            return; // notificação sem external_reference reconhecível — ignora silenciosamente
        }

        Pedido pedido = pedidoRepository.buscarPorIdComItens(UUID.fromString(pagamento.pedidoId()))
                .orElseThrow(() -> new NoSuchElementException(
                        "Pedido " + pagamento.pedidoId() + " não encontrado pro pagamento " + paymentId + "."));

        // Idempotência: já processado (webhook duplicado) ou pedido num estado que não aceita
        // mais confirmação de pagamento (cancelado/reembolsado manualmente) — no-op silencioso.
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            return;
        }
        if (!STATUS_APROVADO.equals(pagamento.status())) {
            // Recusado/pendente — pedido continua em AGUARDANDO_PAGAMENTO, mentorado pode tentar
            // de novo sem perder o carrinho (H8.3, ver Pedido.iniciarCheckout()).
            return;
        }

        pedido.confirmarPagamento();
        pedido.liberar();
        atividadeLogService.registrar("PEDIDO_PAGO", "Pedido pago: " + pedido.getMentorado().getNome());
        pedidoRepository.save(pedido);

        for (ItemPedido item : pedido.getItens()) {
            Produto produto = item.getProduto();
            produto.incrementarVendas(item.getQuantidade());
            produtoRepository.save(produto);
        }

        registrarReceita(pedido);
    }

    // Fecha o contrato já documentado desde o Blueprint do E13: vendasLoja lê de
    // OrigemReceita.LOJA e ficava em R$ 0 "até o E8 existir" — este é o único ponto do sistema
    // que escreve nessa categoria. Sem service intermediário (LancamentoFinanceiroService não
    // existe no projeto) — mesmo padrão direto de ContaPagarReceberService.liquidar().
    private void registrarReceita(Pedido pedido) {
        CategoriaFinanceira categoriaLoja = categoriaFinanceiraRepository.findByOrigemReceita(OrigemReceita.LOJA)
                .orElseThrow(() -> new IllegalStateException(
                        "Categoria financeira de origem LOJA não encontrada — verifique o seed/migração do Financeiro."));
        String descricao = "Pedido " + pedido.getId();
        lancamentoFinanceiroRepository.save(new LancamentoFinanceiro(TipoLancamento.RECEITA, categoriaLoja, descricao,
                pedido.getValorTotal(), LocalDate.now(), StatusLancamento.REALIZADO, null));
    }
}
