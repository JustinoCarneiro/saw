package com.sawhub.hub.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sawhub.hub.common.dto.ImportResultResponse;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

/** M21 — RED primeiro: LancamentoCsvService ainda não existe neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class LancamentoCsvServiceTest {

    @Mock
    private LancamentoService lancamentoService;
    @Mock
    private LancamentoFinanceiroRepository lancamentoRepository;
    @Mock
    private CategoriaFinanceiraRepository categoriaRepository;

    private LancamentoCsvService service() {
        return new LancamentoCsvService(lancamentoService, lancamentoRepository, categoriaRepository);
    }

    private static CategoriaFinanceira categoriaAssinatura() {
        return new CategoriaFinanceira("Assinaturas", TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, OrigemReceita.ASSINATURA);
    }

    private static MockMultipartFile csv(String conteudo) {
        return new MockMultipartFile("arquivo", "lancamentos.csv", "text/csv", conteudo.getBytes(StandardCharsets.UTF_8));
    }

    // --- exportar ---

    @Test
    void exportarProduzCsvComPontoEVirgulaVirgulaDecimalEDataPtBr() {
        CategoriaFinanceira categoria = categoriaAssinatura();
        LancamentoFinanceiro lancamento = new LancamentoFinanceiro(TipoLancamento.RECEITA, categoria,
                "Assinatura João Silva", new BigDecimal("397.50"), LocalDate.of(2026, 7, 10),
                StatusLancamento.REALIZADO, null);
        LocalDate de = LocalDate.of(2026, 7, 1);
        LocalDate ate = LocalDate.of(2026, 7, 31);
        when(lancamentoService.listar(de, ate, null, null)).thenReturn(List.of(lancamento));

        String csv = service().exportar(de, ate, null, null);

        assertThat(csv).contains("tipo;categoria;descricao;valor;dataCompetencia;status;planoReferencia");
        assertThat(csv).contains("RECEITA;Assinaturas;Assinatura João Silva;397,50;10/07/2026;REALIZADO;");
    }

    @Test
    void exportarNeutralizaDescricaoQueComecaComSinalDeFormula() {
        CategoriaFinanceira categoria = categoriaAssinatura();
        LancamentoFinanceiro lancamento = new LancamentoFinanceiro(TipoLancamento.RECEITA, categoria,
                "=SOMA(A1:A2)", new BigDecimal("10.00"), LocalDate.of(2026, 7, 10), StatusLancamento.REALIZADO, null);
        when(lancamentoService.listar(any(), any(), any(), any())).thenReturn(List.of(lancamento));

        String csv = service().exportar(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), null, null);

        assertThat(csv).contains("'=SOMA(A1:A2)");
    }

    // --- importar: caminho feliz ---

    @Test
    void importarPersisteTodasAsLinhasQuandoTudoEValido() {
        CategoriaFinanceira categoria = categoriaAssinatura();
        when(categoriaRepository.findByNomeIgnoreCase("Assinaturas")).thenReturn(List.of(categoria));

        String conteudo = "tipo;categoria;descricao;valor;dataCompetencia;status;planoReferencia\n"
                + "RECEITA;Assinaturas;Mensalidade João;397,50;10/07/2026;REALIZADO;PROFISSIONAL\n"
                + "RECEITA;Assinaturas;Mensalidade Ana;250,00;11/07/2026;PREVISTO;\n";

        ImportResultResponse resultado = service().importar(csv(conteudo));

        assertThat(resultado.totalLinhas()).isEqualTo(2);
        assertThat(resultado.importados()).isEqualTo(2);
        assertThat(resultado.erros()).isEmpty();

        ArgumentCaptor<List<LancamentoFinanceiro>> captor = ArgumentCaptor.forClass(List.class);
        verify(lancamentoRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue().get(0).getValor()).isEqualByComparingTo("397.50");
        assertThat(captor.getValue().get(0).getDescricao()).isEqualTo("Mensalidade João");
    }

    @Test
    void importarDetectaDelimitadorVirgulaEValorComPontoDecimal() {
        CategoriaFinanceira categoria = categoriaAssinatura();
        when(categoriaRepository.findByNomeIgnoreCase("Assinaturas")).thenReturn(List.of(categoria));

        String conteudo = "tipo,categoria,descricao,valor,dataCompetencia,status,planoReferencia\n"
                + "RECEITA,Assinaturas,Mensalidade,397.50,10/07/2026,REALIZADO,\n";

        ImportResultResponse resultado = service().importar(csv(conteudo));

        assertThat(resultado.importados()).isEqualTo(1);
        assertThat(resultado.erros()).isEmpty();
    }

    // --- importar: tudo-ou-nada ---

    @Test
    void importarNaoPersisteNadaQuandoUmaLinhaEhInvalida() {
        CategoriaFinanceira categoria = categoriaAssinatura();
        when(categoriaRepository.findByNomeIgnoreCase("Assinaturas")).thenReturn(List.of(categoria));

        String conteudo = "tipo;categoria;descricao;valor;dataCompetencia;status;planoReferencia\n"
                + "RECEITA;Assinaturas;Mensalidade João;397,50;10/07/2026;REALIZADO;\n"
                + "RECEITA;Assinaturas;Mensalidade Ana;abc;11/07/2026;REALIZADO;\n";

        ImportResultResponse resultado = service().importar(csv(conteudo));

        assertThat(resultado.totalLinhas()).isEqualTo(2);
        assertThat(resultado.importados()).isZero();
        assertThat(resultado.erros()).hasSize(1);
        assertThat(resultado.erros().get(0).linha()).isEqualTo(3);
        assertThat(resultado.erros().get(0).motivo()).contains("abc");

        verify(lancamentoRepository, never()).saveAll(any());
    }

    @Test
    void importarRejeitaCategoriaInexistente() {
        when(categoriaRepository.findByNomeIgnoreCase("Aluguel")).thenReturn(List.of());

        String conteudo = "tipo;categoria;descricao;valor;dataCompetencia;status;planoReferencia\n"
                + "DESPESA;Aluguel;Sede;1000;10/07/2026;REALIZADO;\n";

        ImportResultResponse resultado = service().importar(csv(conteudo));

        assertThat(resultado.erros()).hasSize(1);
        assertThat(resultado.erros().get(0).motivo()).contains("não encontrada");
    }

    @Test
    void importarRejeitaCategoriaAmbigua() {
        when(categoriaRepository.findByNomeIgnoreCase("Assinaturas")).thenReturn(
                List.of(categoriaAssinatura(), categoriaAssinatura()));

        String conteudo = "tipo;categoria;descricao;valor;dataCompetencia;status;planoReferencia\n"
                + "RECEITA;Assinaturas;Mensalidade;397,50;10/07/2026;REALIZADO;\n";

        ImportResultResponse resultado = service().importar(csv(conteudo));

        assertThat(resultado.erros()).hasSize(1);
        assertThat(resultado.erros().get(0).motivo()).contains("ambígua");
    }

    // Achado do Blueprint: o endpoint manual não valida isso, mas o import (superfície nova, sem
    // o <select> filtrado do frontend) precisa recusar categoria de tipo incompatível.
    @Test
    void importarRejeitaCategoriaComTipoIncompativel() {
        CategoriaFinanceira categoriaDespesa = new CategoriaFinanceira("Infra", TipoLancamento.DESPESA, GrupoDre.CUSTOS, null);
        when(categoriaRepository.findByNomeIgnoreCase("Infra")).thenReturn(List.of(categoriaDespesa));

        String conteudo = "tipo;categoria;descricao;valor;dataCompetencia;status;planoReferencia\n"
                + "RECEITA;Infra;Mensalidade;397,50;10/07/2026;REALIZADO;\n";

        ImportResultResponse resultado = service().importar(csv(conteudo));

        assertThat(resultado.erros()).hasSize(1);
        assertThat(resultado.erros().get(0).motivo()).contains("incompatível");
    }

    @Test
    void importarRejeitaArquivoComColunaFaltando() {
        String conteudo = "tipo;categoria;descricao;valor;dataCompetencia\n"
                + "RECEITA;Assinaturas;Mensalidade;397,50;10/07/2026\n";

        assertThatThrownBy(() -> service().importar(csv(conteudo)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("status");
    }

    @Test
    void importarRejeitaArquivoSemLinhasDeDados() {
        String conteudo = "tipo;categoria;descricao;valor;dataCompetencia;status;planoReferencia\n";

        assertThatThrownBy(() -> service().importar(csv(conteudo)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sem nenhuma linha");
    }

    @Test
    void importarRejeitaExtensaoDiferenteDeCsv() {
        var arquivo = new MockMultipartFile("arquivo", "lancamentos.xlsx", "application/vnd.ms-excel", "conteudo".getBytes());

        assertThatThrownBy(() -> service().importar(arquivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(".csv");
    }

    @Test
    void importarRejeitaArquivoVazio() {
        var arquivo = new MockMultipartFile("arquivo", "lancamentos.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> service().importar(arquivo))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void importarRejeitaMaisDe5000Linhas() {
        StringBuilder conteudo = new StringBuilder("tipo;categoria;descricao;valor;dataCompetencia;status;planoReferencia\n");
        for (int i = 0; i < 5001; i++) {
            conteudo.append("RECEITA;Assinaturas;Mensalidade;100;10/07/2026;REALIZADO;\n");
        }

        assertThatThrownBy(() -> service().importar(csv(conteudo.toString())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5000");
    }
}
