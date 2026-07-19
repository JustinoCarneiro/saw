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

/** M21 — RED primeiro: ContaCsvService ainda não existe neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class ContaCsvServiceTest {

    @Mock
    private ContaPagarReceberService contaService;
    @Mock
    private ContaPagarReceberRepository contaRepository;
    @Mock
    private CategoriaFinanceiraRepository categoriaRepository;

    private ContaCsvService service() {
        return new ContaCsvService(contaService, contaRepository, categoriaRepository);
    }

    private static CategoriaFinanceira categoriaInfra() {
        return new CategoriaFinanceira("Infra", TipoLancamento.DESPESA, GrupoDre.CUSTOS, null);
    }

    private static MockMultipartFile csv(String conteudo) {
        return new MockMultipartFile("arquivo", "contas.csv", "text/csv", conteudo.getBytes(StandardCharsets.UTF_8));
    }

    // --- exportar ---

    @Test
    void exportarProduzCsvComPontoEVirgulaVirgulaDecimalEDataPtBr() {
        CategoriaFinanceira categoria = categoriaInfra();
        ContaPagarReceber conta = new ContaPagarReceber(TipoConta.A_PAGAR, "Servidor Hostinger",
                new BigDecimal("180.00"), LocalDate.of(2026, 7, 20), categoria);
        when(contaService.listar(null, null, null, null, null)).thenReturn(List.of(conta));

        String csv = service().exportar(null, null, null, null, null);

        assertThat(csv).contains("tipo;descricao;valor;dataVencimento;categoria");
        assertThat(csv).contains("A_PAGAR;Servidor Hostinger;180,00;20/07/2026;Infra");
    }

    @Test
    void exportarContaSemCategoriaDeixaColunaVazia() {
        ContaPagarReceber conta = new ContaPagarReceber(TipoConta.A_PAGAR, "Diversos",
                new BigDecimal("50.00"), LocalDate.of(2026, 7, 20), null);
        when(contaService.listar(any(), any(), any(), any(), any())).thenReturn(List.of(conta));

        String csv = service().exportar(TipoConta.A_PAGAR, null, null, null, null);

        assertThat(csv).contains("A_PAGAR;Diversos;50,00;20/07/2026;\r\n");
    }

    // --- importar: caminho feliz ---

    @Test
    @SuppressWarnings("unchecked")
    void importarPersisteTodasAsLinhasQuandoTudoEValido() {
        CategoriaFinanceira categoria = categoriaInfra();
        when(categoriaRepository.findByNomeIgnoreCase("Infra")).thenReturn(List.of(categoria));

        String conteudo = "tipo;descricao;valor;dataVencimento;categoria\n"
                + "A_PAGAR;Servidor Hostinger;180,00;20/07/2026;Infra\n"
                + "A_PAGAR;Domínio;45,90;25/07/2026;\n";

        ImportResultResponse resultado = service().importar(csv(conteudo));

        assertThat(resultado.totalLinhas()).isEqualTo(2);
        assertThat(resultado.importados()).isEqualTo(2);
        assertThat(resultado.erros()).isEmpty();

        ArgumentCaptor<List<ContaPagarReceber>> captor = ArgumentCaptor.forClass(List.class);
        verify(contaRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue().get(0).getValor()).isEqualByComparingTo("180.00");
        assertThat(captor.getValue().get(0).getStatus()).isEqualTo(StatusConta.PENDENTE);
        assertThat(captor.getValue().get(1).getCategoria()).isNull();
    }

    // --- importar: tudo-ou-nada ---

    @Test
    void importarNaoPersisteNadaQuandoUmaLinhaEhInvalida() {
        CategoriaFinanceira categoria = categoriaInfra();
        when(categoriaRepository.findByNomeIgnoreCase("Infra")).thenReturn(List.of(categoria));

        String conteudo = "tipo;descricao;valor;dataVencimento;categoria\n"
                + "A_PAGAR;Servidor Hostinger;180,00;20/07/2026;Infra\n"
                + "A_PAGAR;Domínio;45,90;31/13/2026;\n";

        ImportResultResponse resultado = service().importar(csv(conteudo));

        assertThat(resultado.importados()).isZero();
        assertThat(resultado.erros()).hasSize(1);
        assertThat(resultado.erros().get(0).linha()).isEqualTo(3);

        verify(contaRepository, never()).saveAll(any());
    }

    @Test
    void importarRejeitaCategoriaComTipoIncompativel() {
        CategoriaFinanceira categoriaReceita = new CategoriaFinanceira("Assinaturas", TipoLancamento.RECEITA,
                GrupoDre.RECEITA_BRUTA, OrigemReceita.ASSINATURA);
        when(categoriaRepository.findByNomeIgnoreCase("Assinaturas")).thenReturn(List.of(categoriaReceita));

        String conteudo = "tipo;descricao;valor;dataVencimento;categoria\n"
                + "A_PAGAR;Servidor;180,00;20/07/2026;Assinaturas\n";

        ImportResultResponse resultado = service().importar(csv(conteudo));

        assertThat(resultado.erros()).hasSize(1);
        assertThat(resultado.erros().get(0).motivo()).contains("incompatível");
    }

    @Test
    void importarRejeitaArquivoComColunaFaltando() {
        String conteudo = "tipo;descricao;valor;dataVencimento\n"
                + "A_PAGAR;Servidor;180,00;20/07/2026\n";

        assertThatThrownBy(() -> service().importar(csv(conteudo)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("categoria");
    }

    @Test
    void importarRejeitaExtensaoDiferenteDeCsv() {
        var arquivo = new MockMultipartFile("arquivo", "contas.txt", "text/plain", "conteudo".getBytes());

        assertThatThrownBy(() -> service().importar(arquivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(".csv");
    }
}
