package com.sawhub.hub.comercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sawhub.hub.common.dto.ImportResultResponse;
import com.sawhub.hub.mentorado.Plano;
import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

/** M22 — RED primeiro: LeadCsvService ainda não existe neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class LeadCsvServiceTest {

    @Mock
    private LeadService leadService;
    @Mock
    private LeadRepository leadRepository;

    private LeadCsvService service() {
        return new LeadCsvService(leadService, leadRepository);
    }

    private static MockMultipartFile csv(String conteudo) {
        return new MockMultipartFile("arquivo", "leads.csv", "text/csv", conteudo.getBytes(StandardCharsets.UTF_8));
    }

    // --- exportar ---

    @Test
    void exportarLeadEmSolicitacaoDeixaColunasDeFunilVazias() {
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", "11999998888", "Quero saber mais", Plano.ESSENCIAL);
        when(leadService.listar(null, null)).thenReturn(List.of(lead));

        String csv = service().exportar(null, null);

        assertThat(csv).contains("nome;email;telefone;mensagem;planoInteresse;status;vendedor;planoFechado;motivoPerdido;dataFechamento");
        assertThat(csv).contains("Maria Souza;maria@restaurante.com;11999998888;Quero saber mais;ESSENCIAL;SOLICITACAO;;;;");
    }

    @Test
    void exportarLeadFechadoTrazVendedorEDataFormatadaEmPtBr() {
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, Plano.ESSENCIAL);
        Colaborador vendedor = new Colaborador(null, "Paula Mendes", Area.COMERCIAL);
        lead.moverParaEmContato(vendedor);
        lead.moverParaProposta();
        lead.fechar(Plano.ESSENCIAL);
        ReflectionTestUtils.setField(lead, "dataFechamento", Instant.parse("2026-07-10T15:30:00Z"));
        when(leadService.listar(any(), any())).thenReturn(List.of(lead));

        String csv = service().exportar(null, null);

        assertThat(csv).contains("FECHADO;Paula Mendes;ESSENCIAL;;");
        assertThat(csv).contains("10/07/2026");
    }

    @Test
    void exportarNeutralizaNomeQueComecaComSinalDeFormula() {
        Lead lead = new Lead("=SOMA(A1:A2)", "x@x.com", null, null, null);
        when(leadService.listar(any(), any())).thenReturn(List.of(lead));

        String csv = service().exportar(null, null);

        assertThat(csv).contains("'=SOMA(A1:A2)");
    }

    // --- importar: caminho feliz (sempre SOLICITACAO) ---

    @Test
    void importarCriaLeadsNovosSempreEmSolicitacao() {
        String conteudo = "nome;email;telefone;mensagem;planoInteresse\n"
                + "Maria Souza;maria@restaurante.com;11999998888;Quero saber mais;ESSENCIAL\n"
                + "Carlos Lima;carlos@bistro.com;;;\n";

        ImportResultResponse resultado = service().importar(csv(conteudo));

        assertThat(resultado.totalLinhas()).isEqualTo(2);
        assertThat(resultado.importados()).isEqualTo(2);
        assertThat(resultado.erros()).isEmpty();

        ArgumentCaptor<List<Lead>> captor = ArgumentCaptor.forClass(List.class);
        verify(leadRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue().get(0).getStatus()).isEqualTo(StatusLead.SOLICITACAO);
        assertThat(captor.getValue().get(0).getNome()).isEqualTo("Maria Souza");
        assertThat(captor.getValue().get(0).getPlanoInteresse()).isEqualTo(Plano.ESSENCIAL);
        assertThat(captor.getValue().get(1).getTelefone()).isNull();
    }

    // --- importar: tudo-ou-nada ---

    @Test
    void importarNaoPersisteNadaQuandoUmaLinhaEhInvalida() {
        String conteudo = "nome;email;telefone;mensagem;planoInteresse\n"
                + "Maria Souza;maria@restaurante.com;;;\n"
                + "Carlos Lima;email-invalido;;;\n";

        ImportResultResponse resultado = service().importar(csv(conteudo));

        assertThat(resultado.importados()).isZero();
        assertThat(resultado.erros()).hasSize(1);
        assertThat(resultado.erros().get(0).linha()).isEqualTo(3);
        assertThat(resultado.erros().get(0).motivo()).contains("inválido");

        verify(leadRepository, never()).saveAll(any());
    }

    @Test
    void importarRejeitaNomeEmBranco() {
        String conteudo = "nome;email;telefone;mensagem;planoInteresse\n"
                + ";maria@restaurante.com;;;\n";

        ImportResultResponse resultado = service().importar(csv(conteudo));

        assertThat(resultado.erros()).hasSize(1);
        assertThat(resultado.erros().get(0).motivo()).contains("Nome em branco");
    }

    @Test
    void importarRejeitaPlanoInteresseInvalido() {
        String conteudo = "nome;email;telefone;mensagem;planoInteresse\n"
                + "Maria Souza;maria@restaurante.com;;;PLANO_INEXISTENTE\n";

        ImportResultResponse resultado = service().importar(csv(conteudo));

        assertThat(resultado.erros()).hasSize(1);
        assertThat(resultado.erros().get(0).motivo()).contains("Plano de interesse");
    }

    @Test
    void importarRejeitaArquivoComColunaFaltando() {
        String conteudo = "nome;email\nMaria;maria@restaurante.com\n";

        assertThatThrownBy(() -> service().importar(csv(conteudo)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("telefone");
    }

    @Test
    void importarRejeitaMensagemAcimaDe500Caracteres() {
        String mensagemGigante = "x".repeat(501);
        String conteudo = "nome;email;telefone;mensagem;planoInteresse\n"
                + "Maria Souza;maria@restaurante.com;;" + mensagemGigante + ";\n";

        ImportResultResponse resultado = service().importar(csv(conteudo));

        assertThat(resultado.erros()).hasSize(1);
        assertThat(resultado.erros().get(0).motivo()).contains("500 caracteres");
    }
}
