package com.sawhub.hub.loja;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sawhub.hub.loja.dto.AtualizarProdutoRequest;
import com.sawhub.hub.loja.dto.CriarProdutoRequest;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;

    private ProdutoService service() {
        return new ProdutoService(produtoRepository);
    }

    private static Produto produto(UUID id) {
        Produto p = new Produto("Planilha", "desc", CategoriaProduto.PLANILHA, new BigDecimal("100.00"), null, null,
                false, "https://cdn.sawhub.com.br/x.zip", null, false);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    @Test
    void criarPersisteProdutoDespublicado() {
        when(produtoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new CriarProdutoRequest("Planilha", "desc", CategoriaProduto.PLANILHA, new BigDecimal("100.00"),
                null, null, false, "https://cdn.sawhub.com.br/x.zip", null, false);

        Produto criado = service().criar(request);

        assertThat(criado.isPublicado()).isFalse();
        assertThat(criado.getTitulo()).isEqualTo("Planilha");
    }

    @Test
    void publicarAlternaFlag() {
        UUID id = UUID.randomUUID();
        when(produtoRepository.findById(id)).thenReturn(Optional.of(produto(id)));
        when(produtoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Produto atualizado = service().publicar(id);

        assertThat(atualizado.isPublicado()).isTrue();
    }

    @Test
    void atualizarProdutoInexistenteLancaErro() {
        UUID id = UUID.randomUUID();
        when(produtoRepository.findById(id)).thenReturn(Optional.empty());

        var request = new AtualizarProdutoRequest("Planilha", "desc", CategoriaProduto.PLANILHA,
                new BigDecimal("100.00"), null, null, false, "https://cdn.sawhub.com.br/x.zip", null, false);

        assertThatThrownBy(() -> service().atualizar(id, request)).isInstanceOf(IllegalArgumentException.class);
    }
}
