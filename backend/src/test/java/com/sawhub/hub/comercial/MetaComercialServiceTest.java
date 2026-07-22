package com.sawhub.hub.comercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sawhub.hub.comercial.dto.CriarMetaComercialRequest;
import com.sawhub.hub.comercial.dto.MetaComercialResponse;
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

/** Pedido do Marcos (22/07/2026) — até esta leva, MetaComercial só existia via DemoDataSeeder,
 * sem service/controller/tela pra criar ou editar. */
@ExtendWith(MockitoExtension.class)
class MetaComercialServiceTest {

    @Mock
    private MetaComercialRepository metaComercialRepository;
    @Mock
    private ColaboradorRepository colaboradorRepository;

    private MetaComercialService service() {
        return new MetaComercialService(metaComercialRepository, colaboradorRepository);
    }

    private static Colaborador colaborador(UUID id, String nome) {
        Colaborador c = new Colaborador(null, nome, Area.COMERCIAL);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    @Test
    void definirComVendedorInexistenteLancaErro() {
        UUID vendedorId = UUID.randomUUID();
        when(colaboradorRepository.findById(vendedorId)).thenReturn(Optional.empty());

        var request = new CriarMetaComercialRequest(vendedorId, 2026, 7, 5, null);

        assertThatThrownBy(() -> service().definir(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vendedor não encontrado");
    }

    @Test
    void definirSemMetaExistenteCriaNova() {
        UUID vendedorId = UUID.randomUUID();
        Colaborador vendedor = colaborador(vendedorId, "Paula Mendes");
        when(colaboradorRepository.findById(vendedorId)).thenReturn(Optional.of(vendedor));
        when(metaComercialRepository.findByVendedorIdAndAnoAndMes(vendedorId, 2026, 7)).thenReturn(Optional.empty());
        when(metaComercialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new CriarMetaComercialRequest(vendedorId, 2026, 7, 5, null);
        MetaComercial meta = service().definir(request);

        assertThat(meta.getVendedor()).isSameAs(vendedor);
        assertThat(meta.getMetaFechamentos()).isEqualTo(5);
    }

    // Upsert — definir de novo a meta do mesmo (vendedor, ano, mês) corrige o valor, não duplica
    // linha (mesmo critério de CaixaMensalService.registrarPosicao).
    @Test
    void definirComMetaExistenteAtualizaEmVezDeDuplicar() {
        UUID vendedorId = UUID.randomUUID();
        Colaborador vendedor = colaborador(vendedorId, "Paula Mendes");
        metaComercialRepository.save(new MetaComercial(vendedor, 2026, 8, 5, null));
        MetaComercial existente = new MetaComercial(vendedor, 2026, 7, 5, null);
        when(colaboradorRepository.findById(vendedorId)).thenReturn(Optional.of(vendedor));
        when(metaComercialRepository.findByVendedorIdAndAnoAndMes(vendedorId, 2026, 7)).thenReturn(Optional.of(existente));
        when(metaComercialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new CriarMetaComercialRequest(vendedorId, 2026, 7, 8, null);
        MetaComercial meta = service().definir(request);

        assertThat(meta).isSameAs(existente);
        assertThat(meta.getMetaFechamentos()).isEqualTo(8);
    }

    @Test
    void listarDevolveMetasDoPeriodo() {
        Colaborador vendedor = colaborador(UUID.randomUUID(), "Paula Mendes");
        when(metaComercialRepository.buscarComVendedorPorPeriodo(2026, 7))
                .thenReturn(List.of(new MetaComercial(vendedor, 2026, 7, 5, null)));

        List<MetaComercialResponse> metas = service().listar(2026, 7);

        assertThat(metas).hasSize(1);
        assertThat(metas.get(0).vendedorNome()).isEqualTo("Paula Mendes");
        assertThat(metas.get(0).metaFechamentos()).isEqualTo(5);
    }
}
