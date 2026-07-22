package com.sawhub.hub.comercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sawhub.hub.common.dto.ImportResultResponse;
import com.sawhub.hub.evento.Evento;
import com.sawhub.hub.evento.EventoRepository;
import com.sawhub.hub.evento.TipoEvento;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

/** Change request pós-MVP (E13, "importação de planilhas de eventos passados pra popular
 * histórico"). */
@ExtendWith(MockitoExtension.class)
class VendaIngressoCsvServiceTest {

    @Mock
    private EventoRepository eventoRepository;
    @Mock
    private LeadRepository leadRepository;
    @Mock
    private VendaIngressoRepository vendaIngressoRepository;

    private VendaIngressoCsvService service() {
        return new VendaIngressoCsvService(eventoRepository, leadRepository, vendaIngressoRepository);
    }

    private static MockMultipartFile csv(String conteudo) {
        return new MockMultipartFile("arquivo", "vendas.csv", "text/csv", conteudo.getBytes(StandardCharsets.UTF_8));
    }

    private static Evento evento() {
        return new Evento("Receita do Sucesso", TipoEvento.PRESENCIAL, null, Instant.now(), "Recife", null, 200);
    }

    @Test
    void importarEventoInexistenteLancaErro() {
        UUID eventoId = UUID.randomUUID();
        when(eventoRepository.findById(eventoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().importar(eventoId, csv("nomeAluno;quantidadeIngressos\nJoão;1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Evento não encontrado");
        verify(vendaIngressoRepository, never()).saveAll(any());
    }

    @Test
    void importarCriaUmaVendaIngressoPorUnidadeDeQuantidade() {
        UUID eventoId = UUID.randomUUID();
        Evento evento = evento();
        when(eventoRepository.findById(eventoId)).thenReturn(Optional.of(evento));
        when(leadRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        String conteudo = "nomeAluno;quantidadeIngressos;valorLiquidoIngresso;tipoIngresso;origemVenda;email\n"
                + "João Comprador;2;150,00;VIP;HOTMART;joao@restaurante.com";

        ImportResultResponse resultado = service().importar(eventoId, csv(conteudo));

        assertThat(resultado.erros()).isEmpty();
        assertThat(resultado.importados()).isEqualTo(2); // 2 ingressos (quantidade=2), não 2 linhas

        ArgumentCaptor<List<Lead>> leadCaptor = ArgumentCaptor.forClass(List.class);
        verify(leadRepository).saveAll(leadCaptor.capture());
        List<Lead> leadsSalvos = leadCaptor.getValue();
        assertThat(leadsSalvos).hasSize(1);
        Lead lead = leadsSalvos.get(0);
        assertThat(lead.getStatus()).isEqualTo(StatusLead.FECHADO);
        assertThat(lead.getOrigemVenda()).isEqualTo(OrigemVenda.HOTMART);
        // valorTotalVenda = valorLiquido (150) * quantidade (2) = 300.
        assertThat(lead.getValorTotalVenda()).isEqualByComparingTo("300.00");

        ArgumentCaptor<List<VendaIngresso>> vendaCaptor = ArgumentCaptor.forClass(List.class);
        verify(vendaIngressoRepository).saveAll(vendaCaptor.capture());
        assertThat(vendaCaptor.getValue()).hasSize(2);
        assertThat(vendaCaptor.getValue()).allSatisfy(v -> {
            assertThat(v.getCategoriaIngresso()).isEqualTo(CategoriaIngresso.VIP);
            assertThat(v.getEvento()).isSameAs(evento);
            assertThat(v.getEmail()).isEqualTo("joao@restaurante.com");
        });
    }

    @Test
    void importarSemEmailValidoGeraErroDeLinhaSemPersistirNada() {
        UUID eventoId = UUID.randomUUID();
        when(eventoRepository.findById(eventoId)).thenReturn(Optional.of(evento()));

        String conteudo = "nomeAluno;quantidadeIngressos;valorLiquidoIngresso;tipoIngresso;email\n"
                + "João Comprador;1;150,00;VIP;nao-e-email";

        ImportResultResponse resultado = service().importar(eventoId, csv(conteudo));

        assertThat(resultado.erros()).hasSize(1);
        assertThat(resultado.erros().get(0).motivo()).contains("E-mail");
        assertThat(resultado.importados()).isZero();
        verify(leadRepository, never()).saveAll(any());
        verify(vendaIngressoRepository, never()).saveAll(any());
    }

    // Achado médio do revisor-seguranca: sem teto, uma célula minúscula ("999999999") tentaria
    // alocar um ArrayList com ~2 bilhões de posições (OutOfMemoryError, não uma linha de erro
    // limpa) — QUANTIDADE_MAXIMA (500) barra isso antes de qualquer alocação.
    @Test
    void importarComQuantidadeAcimaDoMaximoGeraErroDeLinha() {
        UUID eventoId = UUID.randomUUID();
        when(eventoRepository.findById(eventoId)).thenReturn(Optional.of(evento()));

        String conteudo = "nomeAluno;quantidadeIngressos;valorLiquidoIngresso;tipoIngresso;email\n"
                + "João Comprador;999999999;150,00;VIP;joao@restaurante.com";

        ImportResultResponse resultado = service().importar(eventoId, csv(conteudo));

        assertThat(resultado.erros()).hasSize(1);
        assertThat(resultado.erros().get(0).motivo()).contains("máximo");
        verify(vendaIngressoRepository, never()).saveAll(any());
    }

    // Achado baixo do revisor-seguranca: path CSV precisa espelhar os limites de tamanho do path
    // JSON (VendaIngressoRequest) — senão o erro só aparece como 409 genérico no save(), em vez
    // de um erro de linha claro pro usuário corrigir o arquivo.
    @Test
    void importarComNomeDeEmpresaAcimaDoLimiteGeraErroDeLinha() {
        UUID eventoId = UUID.randomUUID();
        when(eventoRepository.findById(eventoId)).thenReturn(Optional.of(evento()));

        String nomeEmpresaGigante = "A".repeat(256);
        String conteudo = "nomeAluno;quantidadeIngressos;valorLiquidoIngresso;tipoIngresso;nomeEmpresa;email\n"
                + "João Comprador;1;150,00;VIP;" + nomeEmpresaGigante + ";joao@restaurante.com";

        ImportResultResponse resultado = service().importar(eventoId, csv(conteudo));

        assertThat(resultado.erros()).hasSize(1);
        assertThat(resultado.erros().get(0).motivo()).contains("Nome da empresa");
        verify(vendaIngressoRepository, never()).saveAll(any());
    }

    // "Origem da Venda" real mistura canal (CORTESIA/HOTMART/PARCEIRO) com nome de vendedora — só
    // os 3 canais têm valor de enum equivalente, qualquer outro texto vira DIRETA.
    @Test
    void importarComOrigemDeVendedoraViraDireta() {
        UUID eventoId = UUID.randomUUID();
        when(eventoRepository.findById(eventoId)).thenReturn(Optional.of(evento()));
        when(leadRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        String conteudo = "nomeAluno;quantidadeIngressos;valorLiquidoIngresso;tipoIngresso;origemVenda;email\n"
                + "Ana Sócia;1;200,00;ESSENCIAL;Aline Melo Comercial;ana@restaurante.com";

        service().importar(eventoId, csv(conteudo));

        ArgumentCaptor<List<Lead>> leadCaptor = ArgumentCaptor.forClass(List.class);
        verify(leadRepository).saveAll(leadCaptor.capture());
        List<Lead> leadsSalvos = leadCaptor.getValue();
        assertThat(leadsSalvos.get(0).getOrigemVenda()).isEqualTo(OrigemVenda.DIRETA);
    }

    @Test
    void importarSemColunaObrigatoriaLancaErro() {
        UUID eventoId = UUID.randomUUID();
        when(eventoRepository.findById(eventoId)).thenReturn(Optional.of(evento()));

        assertThatThrownBy(() -> service().importar(eventoId, csv("nomeAluno\nJoão")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("coluna");
    }
}
