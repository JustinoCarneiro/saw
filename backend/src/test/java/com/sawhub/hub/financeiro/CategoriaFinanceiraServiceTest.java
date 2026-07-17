package com.sawhub.hub.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sawhub.hub.financeiro.dto.CriarCategoriaFinanceiraRequest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/** Fase 5 (H14.1) — RED primeiro: CategoriaFinanceiraService ainda não existe neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class CategoriaFinanceiraServiceTest {

    @Mock
    private CategoriaFinanceiraRepository categoriaRepository;

    private CategoriaFinanceiraService service() {
        return new CategoriaFinanceiraService(categoriaRepository);
    }

    @Test
    void criarPersisteCategoriaComOsDadosInformados() {
        when(categoriaRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new CriarCategoriaFinanceiraRequest("Mensalidades", TipoLancamento.RECEITA,
                GrupoDre.RECEITA_BRUTA, OrigemReceita.ASSINATURA);
        CategoriaFinanceira categoria = service().criar(request);

        assertThat(categoria.getNome()).isEqualTo("Mensalidades");
        assertThat(categoria.getTipo()).isEqualTo(TipoLancamento.RECEITA);
        assertThat(categoria.getGrupoDre()).isEqualTo(GrupoDre.RECEITA_BRUTA);
        assertThat(categoria.getOrigemReceita()).isEqualTo(OrigemReceita.ASSINATURA);
    }

    @Test
    void criarDespesaSemOrigemReceitaFunciona() {
        when(categoriaRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new CriarCategoriaFinanceiraRequest("Folha de pagamento", TipoLancamento.DESPESA,
                GrupoDre.DESPESA_OPERACIONAL, null);
        CategoriaFinanceira categoria = service().criar(request);

        assertThat(categoria.getOrigemReceita()).isNull();
    }

    @Test
    void criarComOrigemReceitaJaUsadaLancaErro() {
        // PedidoPagamentoService resolve a categoria da Loja via findByOrigemReceita(LOJA) esperando
        // 0 ou 1 resultado — uma segunda categoria com a mesma origem quebraria essa busca.
        when(categoriaRepository.findByOrigemReceita(OrigemReceita.LOJA))
                .thenReturn(Optional.of(new CategoriaFinanceira("Loja", TipoLancamento.RECEITA,
                        GrupoDre.RECEITA_BRUTA, OrigemReceita.LOJA)));

        var request = new CriarCategoriaFinanceiraRequest("Loja duplicada", TipoLancamento.RECEITA,
                GrupoDre.RECEITA_BRUTA, OrigemReceita.LOJA);

        assertThatThrownBy(() -> service().criar(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LOJA");
    }

    @Test
    void criarComOrigemReceitaEmCorridaDeConcorrenciaLancaErroDeConflito() {
        // V22 (achado do revisor-seguranca): duas chamadas concorrentes passam ambas pela checagem
        // em Java (nenhuma vê a outra ainda não commitada) — quem chega no banco por último esbarra
        // no índice único uq_categoria_financeira_origem_receita e precisa virar erro de negócio
        // (409), não vazar como exceção de persistência crua (500).
        when(categoriaRepository.findByOrigemReceita(OrigemReceita.LOJA)).thenReturn(Optional.empty());
        when(categoriaRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        var request = new CriarCategoriaFinanceiraRequest("Loja concorrente", TipoLancamento.RECEITA,
                GrupoDre.RECEITA_BRUTA, OrigemReceita.LOJA);

        assertThatThrownBy(() -> service().criar(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LOJA");
    }
}
