package com.sawhub.hub.loja;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sawhub.hub.atividade.AtividadeLogService;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.Plano;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PedidoAdminServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private AtividadeLogService atividadeLogService;

    private PedidoAdminService service() {
        return new PedidoAdminService(pedidoRepository, atividadeLogService);
    }

    private static Pedido pedidoLiberado() {
        Mentorado m = new Mentorado(null, "Maria", null, Plano.ESSENCIAL, BigDecimal.ZERO, 0, 0);
        Produto produto = new Produto("X", "desc", CategoriaProduto.EBOOK, new BigDecimal("10.00"), null, null,
                false, "https://cdn.sawhub.com.br/x.zip", null);
        Pedido pedido = new Pedido(m);
        pedido.adicionarItem(produto, 1);
        pedido.iniciarCheckout("pref-1");
        pedido.confirmarPagamento();
        pedido.liberar();
        return pedido;
    }

    @Test
    void reembolsarPedidoLiberadoFunciona() {
        UUID id = UUID.randomUUID();
        Pedido pedido = pedidoLiberado();
        when(pedidoRepository.buscarPorIdComItens(id)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Pedido resultado = service().reembolsar(id);

        assertThat(resultado.getStatus()).isEqualTo(StatusPedido.REEMBOLSADO);
        verify(atividadeLogService).registrar("PEDIDO_REEMBOLSADO", "Pedido reembolsado: Maria");
    }

    @Test
    void reembolsarCarrinhoLancaErro() {
        UUID id = UUID.randomUUID();
        Pedido carrinho = new Pedido(new Mentorado(null, "Maria", null, Plano.ESSENCIAL, BigDecimal.ZERO, 0, 0));
        when(pedidoRepository.buscarPorIdComItens(id)).thenReturn(Optional.of(carrinho));

        assertThatThrownBy(() -> service().reembolsar(id)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancelarPedidoInexistenteLancaErro() {
        UUID id = UUID.randomUUID();
        when(pedidoRepository.buscarPorIdComItens(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().cancelar(id)).isInstanceOf(IllegalArgumentException.class);
    }
}
