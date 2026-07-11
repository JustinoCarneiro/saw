package com.sawhub.hub.comercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sawhub.hub.comercial.dto.AvancarLeadRequest;
import com.sawhub.hub.comercial.dto.CriarLeadRequest;
import com.sawhub.hub.mentorado.Plano;
import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
import com.sawhub.hub.team.ColaboradorRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** H1.3 + H13.2 — RED primeiro: LeadService ainda não existe neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;
    @Mock
    private ColaboradorRepository colaboradorRepository;

    private LeadService service() {
        return new LeadService(leadRepository, colaboradorRepository);
    }

    private static Colaborador colaborador(UUID id, String nome) {
        Colaborador c = new Colaborador(null, nome, Area.COMERCIAL);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    @Test
    void criarPersisteLeadEmSolicitacao() {
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new CriarLeadRequest("Maria Souza", "maria@restaurante.com", "11999998888",
                "Quero saber mais", Plano.ESSENCIAL);

        Lead lead = service().criar(request);

        assertThat(lead.getStatus()).isEqualTo(StatusLead.SOLICITACAO);
        assertThat(lead.getNome()).isEqualTo("Maria Souza");
        assertThat(lead.getPlanoInteresse()).isEqualTo(Plano.ESSENCIAL);
    }

    @Test
    void listarDelegaFiltroParaOBancoEmVezDeFiltrarEmMemoria() {
        // Achado M1 da revisão de segurança: o filtro precisa acontecer em SQL (usando os
        // índices idx_lead_status/idx_lead_vendedor), não trazer a tabela inteira pro heap.
        UUID vendedorId = UUID.randomUUID();
        when(leadRepository.buscarComFiltro(StatusLead.PROPOSTA, vendedorId)).thenReturn(List.of());

        service().listar(StatusLead.PROPOSTA, vendedorId);

        verify(leadRepository).buscarComFiltro(StatusLead.PROPOSTA, vendedorId);
    }

    @Test
    void listarSemFiltroPassaNulosAdiante() {
        when(leadRepository.buscarComFiltro(isNull(), isNull())).thenReturn(List.of());

        service().listar(null, null);

        verify(leadRepository).buscarComFiltro(eq((StatusLead) null), eq((UUID) null));
    }

    @Test
    void avancarParaEmContatoAtribuiVendedor() {
        UUID leadId = UUID.randomUUID();
        UUID vendedorId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        Colaborador vendedor = colaborador(vendedorId, "Paula");
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(colaboradorRepository.findById(vendedorId)).thenReturn(Optional.of(vendedor));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Lead avancado = service().avancar(leadId, new AvancarLeadRequest(StatusLead.EM_CONTATO, vendedorId, null, null));

        assertThat(avancado.getStatus()).isEqualTo(StatusLead.EM_CONTATO);
        assertThat(avancado.getVendedor()).isSameAs(vendedor);
    }

    @Test
    void avancarParaEmContatoSemVendedorLancaErro() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));

        assertThatThrownBy(() -> service().avancar(leadId, new AvancarLeadRequest(StatusLead.EM_CONTATO, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vendedor");
    }

    @Test
    void avancarParaEmContatoComVendedorInexistenteLancaErro() {
        UUID leadId = UUID.randomUUID();
        UUID vendedorId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(colaboradorRepository.findById(vendedorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().avancar(leadId, new AvancarLeadRequest(StatusLead.EM_CONTATO, vendedorId, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    void avancarParaPropostaExigeEmContato() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        lead.moverParaEmContato(colaborador(UUID.randomUUID(), "Paula"));
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Lead avancado = service().avancar(leadId, new AvancarLeadRequest(StatusLead.PROPOSTA, null, null, null));

        assertThat(avancado.getStatus()).isEqualTo(StatusLead.PROPOSTA);
    }

    @Test
    void avancarParaFechadoRegistraPlanoFechado() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        lead.moverParaEmContato(colaborador(UUID.randomUUID(), "Paula"));
        lead.moverParaProposta();
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Lead avancado = service().avancar(leadId, new AvancarLeadRequest(StatusLead.FECHADO, null, Plano.ESSENCIAL, null));

        assertThat(avancado.getStatus()).isEqualTo(StatusLead.FECHADO);
        assertThat(avancado.getPlanoFechado()).isEqualTo(Plano.ESSENCIAL);
        assertThat(avancado.getDataFechamento()).isNotNull();
    }

    @Test
    void avancarParaPerdidoRegistraMotivo() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Lead avancado = service().avancar(leadId,
                new AvancarLeadRequest(StatusLead.PERDIDO, null, null, "Optou por concorrente"));

        assertThat(avancado.getStatus()).isEqualTo(StatusLead.PERDIDO);
        assertThat(avancado.getMotivoPerdido()).isEqualTo("Optou por concorrente");
    }

    @Test
    void avancarPulandoEtapaLancaErro() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));

        assertThatThrownBy(() -> service().avancar(leadId, new AvancarLeadRequest(StatusLead.FECHADO, null, Plano.ESSENCIAL, null)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void avancarLeadInexistenteLancaErro() {
        UUID leadId = UUID.randomUUID();
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().avancar(leadId, new AvancarLeadRequest(StatusLead.PERDIDO, null, null, "x")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    void avancarParaFechadoSemPlanoLancaErro() {
        // Achado L2 da revisão de segurança: sem esta checagem, um FECHADO sem plano quebra a
        // atribuição de "vendas por plano" (H13.1) silenciosamente.
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        lead.moverParaEmContato(colaborador(UUID.randomUUID(), "Paula"));
        lead.moverParaProposta();
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));

        assertThatThrownBy(() -> service().avancar(leadId, new AvancarLeadRequest(StatusLead.FECHADO, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plano");
    }

    @Test
    void avancarParaPerdidoSemMotivoLancaErro() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));

        assertThatThrownBy(() -> service().avancar(leadId, new AvancarLeadRequest(StatusLead.PERDIDO, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Motivo");
    }

    @Test
    void avancarParaPerdidoComMotivoEmBrancoLancaErro() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));

        assertThatThrownBy(() -> service().avancar(leadId, new AvancarLeadRequest(StatusLead.PERDIDO, null, null, "   ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Motivo");
    }
}
