package com.sawhub.hub.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sawhub.hub.financeiro.dto.CriarLancamentoRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** H14.1 — RED primeiro: LancamentoService ainda não existe neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class LancamentoServiceTest {

    @Mock
    private LancamentoFinanceiroRepository lancamentoRepository;
    @Mock
    private CategoriaFinanceiraRepository categoriaRepository;

    private LancamentoService service() {
        return new LancamentoService(lancamentoRepository, categoriaRepository);
    }

    private static CategoriaFinanceira categoriaAssinatura() {
        return new CategoriaFinanceira("Assinaturas", TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, OrigemReceita.ASSINATURA);
    }

    @Test
    void criarRejeitaCategoriaInexistente() {
        UUID categoriaId = UUID.randomUUID();
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.empty());
        var request = new CriarLancamentoRequest(TipoLancamento.RECEITA, categoriaId, "Assinatura João Silva",
                new BigDecimal("397.00"), LocalDate.of(2026, 7, 1), StatusLancamento.REALIZADO, null);

        assertThatThrownBy(() -> service().criar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Categoria");
    }

    @Test
    void criarPersisteLancamentoComOsDadosDaRequest() {
        UUID categoriaId = UUID.randomUUID();
        CategoriaFinanceira categoria = categoriaAssinatura();
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(lancamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new CriarLancamentoRequest(TipoLancamento.RECEITA, categoriaId, "Assinatura João Silva",
                new BigDecimal("397.00"), LocalDate.of(2026, 7, 1), StatusLancamento.REALIZADO, null);

        LancamentoFinanceiro criado = service().criar(request);

        assertThat(criado.getTipo()).isEqualTo(TipoLancamento.RECEITA);
        assertThat(criado.getCategoria()).isEqualTo(categoria);
        assertThat(criado.getDescricao()).isEqualTo("Assinatura João Silva");
        assertThat(criado.getValor()).isEqualByComparingTo("397.00");
        assertThat(criado.getStatus()).isEqualTo(StatusLancamento.REALIZADO);
    }

    @Test
    void listarFiltraPorPeriodoTipoECategoria() {
        CategoriaFinanceira categoriaAlvo = categoriaAssinatura();
        CategoriaFinanceira outraCategoria = new CategoriaFinanceira("Loja", TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, OrigemReceita.LOJA);
        UUID categoriaAlvoId = UUID.randomUUID();
        org.springframework.test.util.ReflectionTestUtils.setField(categoriaAlvo, "id", categoriaAlvoId);
        org.springframework.test.util.ReflectionTestUtils.setField(outraCategoria, "id", UUID.randomUUID());

        LocalDate de = LocalDate.of(2026, 7, 1);
        LocalDate ate = LocalDate.of(2026, 7, 31);

        var doTipoCertoDaCategoriaCerta = new LancamentoFinanceiro(TipoLancamento.RECEITA, categoriaAlvo,
                "Bate com o filtro", new BigDecimal("100"), LocalDate.of(2026, 7, 10), StatusLancamento.REALIZADO, null);
        var deOutraCategoria = new LancamentoFinanceiro(TipoLancamento.RECEITA, outraCategoria,
                "Categoria errada", new BigDecimal("50"), LocalDate.of(2026, 7, 10), StatusLancamento.REALIZADO, null);
        var deOutroTipo = new LancamentoFinanceiro(TipoLancamento.DESPESA, categoriaAlvo,
                "Tipo errado", new BigDecimal("30"), LocalDate.of(2026, 7, 10), StatusLancamento.REALIZADO, null);

        when(lancamentoRepository.findByDataCompetenciaBetweenOrderByDataCompetenciaDesc(de, ate))
                .thenReturn(List.of(doTipoCertoDaCategoriaCerta, deOutraCategoria, deOutroTipo));

        List<LancamentoFinanceiro> resultado = service().listar(de, ate, TipoLancamento.RECEITA, categoriaAlvoId);

        assertThat(resultado).containsExactly(doTipoCertoDaCategoriaCerta);
    }
}
