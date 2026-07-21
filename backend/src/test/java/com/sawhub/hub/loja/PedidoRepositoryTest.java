package com.sawhub.hub.loja;

import static org.assertj.core.api.Assertions.assertThat;

import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.security.Perfil;
import com.sawhub.hub.security.Usuario;
import com.sawhub.hub.security.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/** Achado ao vivo (print do cliente): "Cancelar" pedido em Comercial → Loja — Pedidos devolvia
 * "Não foi possível concluir a ação." (500) pra TODO pedido. {@code buscarPorIdComItens()} não
 * fazia FETCH JOIN em {@code mentorado} (@ManyToOne LAZY, open-in-view=false); o service
 * ({@code PedidoAdminService.cancelar()}/{@code reembolsar()}) é @Transactional e devolve o
 * Pedido, mas quem lê {@code pedido.getMentorado().getNome()} é {@code PedidoAdminResponse.from()}
 * no CONTROLLER — já fora da transação. Mesma classe de bug do M12/Fase 5 (ver
 * EncaminhamentoRepositoryTest/MetaRepositoryTest); @DataJpaTest (sessão real do Hibernate) de
 * propósito — um teste baseado em mock (ver PedidoAdminServiceTest, que usa `new Mentorado(...)`
 * direto, nunca um proxy LAZY de verdade) nunca reproduziria isso. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class PedidoRepositoryTest {

    @Autowired
    private PedidoRepository pedidoRepository;
    @Autowired
    private ProdutoRepository produtoRepository;
    @Autowired
    private MentoradoRepository mentoradoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void buscarPorIdComItensContinuaLegivelForaDaTransacaoOriginal() {
        Usuario usuario = usuarioRepository.save(
                new Usuario("mentoradopedido1@sawhub.com.br", "hash", Perfil.MENTORADO));
        Mentorado mentorado = mentoradoRepository.save(
                new Mentorado(usuario, "Mentorado Pedido", null, BigDecimal.ZERO, 0, 0));
        Produto produto = produtoRepository.save(new Produto("Produto X", "desc", CategoriaProduto.EBOOK,
                new BigDecimal("10.00"), null, null, false, "https://cdn.sawhub.com.br/x.zip", null, false));

        Pedido pedido = new Pedido(mentorado);
        pedido.adicionarItem(produto, 1);
        pedidoRepository.save(pedido);
        entityManager.flush();
        entityManager.clear();

        // Simula o fim da transação de PedidoAdminService.cancelar()/reembolsar() (cada chamada de
        // repositório é sua própria transação em produção) ANTES de ler mentorado — sem isto, o bug
        // não reproduz (a sessão ainda estaria aberta).
        Pedido carregado = pedidoRepository.buscarPorIdComItens(pedido.getId()).orElseThrow();
        entityManager.clear();

        assertThat(carregado.getMentorado().getNome()).isEqualTo("Mentorado Pedido");
        assertThat(carregado.getItens()).hasSize(1);
    }
}
