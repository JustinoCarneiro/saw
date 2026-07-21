package com.sawhub.hub.aviso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sawhub.hub.aviso.dto.AvisoMentoradoResponse;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.mentorado.Plano;
import com.sawhub.hub.security.Perfil;
import com.sawhub.hub.security.Usuario;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** H12.1 — RED primeiro: AvisoMentoradoService ainda não existia neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class AvisoMentoradoServiceTest {

    @Mock
    private AvisoMentoradoRepository avisoMentoradoRepository;
    @Mock
    private AvisoRepository avisoRepository;
    @Mock
    private MentoradoRepository mentoradoRepository;

    private AvisoMentoradoService service() {
        return new AvisoMentoradoService(avisoMentoradoRepository, avisoRepository, mentoradoRepository);
    }

    private static Mentorado mentorado(UUID id, Plano plano) {
        Usuario usuario = new Usuario("ana@x.com", "hash", Perfil.MENTORADO);
        Mentorado m = new Mentorado(usuario, "Ana", "Negócio", plano, BigDecimal.ZERO, 0, 0);
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    private static Aviso aviso(String titulo, CategoriaAviso categoria, Plano planoMinimo) {
        Aviso a = new Aviso(titulo, "desc", categoria, planoMinimo);
        ReflectionTestUtils.setField(a, "id", UUID.randomUUID());
        return a;
    }

    @Test
    void listarIsolaPorTenantResolvendoSoPeloUsuarioAutenticado() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID(), Plano.BASICO);
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));

        Aviso a1 = aviso("Aviso 1", CategoriaAviso.GERAL, Plano.GRATUITO);
        when(avisoMentoradoRepository.buscarParaMentorado(eq(mentorado.getId()), isNull()))
                .thenReturn(List.<Object[]>of(new Object[]{a1, null}));

        List<AvisoMentoradoResponse> resposta = service().listar(usuarioId, null, null);

        assertThat(resposta).hasSize(1);
        assertThat(resposta.get(0).titulo()).isEqualTo("Aviso 1");
        assertThat(resposta.get(0).lido()).isFalse();
    }

    @Test
    void listarApenasNaoLidosFiltraOsJaLidos() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID(), Plano.GRATUITO);
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));

        Aviso lido = aviso("Lido", CategoriaAviso.GERAL, Plano.GRATUITO);
        Aviso naoLido = aviso("Não lido", CategoriaAviso.GERAL, Plano.GRATUITO);
        AvisoMentorado am = new AvisoMentorado(mentorado, lido);
        am.marcarLido();
        when(avisoMentoradoRepository.buscarParaMentorado(eq(mentorado.getId()), isNull()))
                .thenReturn(List.<Object[]>of(new Object[]{lido, am}, new Object[]{naoLido, null}));

        List<AvisoMentoradoResponse> resposta = service().listar(usuarioId, null, true);

        assertThat(resposta).hasSize(1);
        assertThat(resposta.get(0).titulo()).isEqualTo("Não lido");
    }

    @Test
    void resumoContaSoOsNaoLidos() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID(), Plano.GRATUITO);
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));

        Aviso lido = aviso("Lido", CategoriaAviso.GERAL, Plano.GRATUITO);
        Aviso naoLido1 = aviso("Não lido 1", CategoriaAviso.GERAL, Plano.GRATUITO);
        Aviso naoLido2 = aviso("Não lido 2", CategoriaAviso.GERAL, Plano.GRATUITO);
        AvisoMentorado am = new AvisoMentorado(mentorado, lido);
        am.marcarLido();
        when(avisoMentoradoRepository.buscarParaMentorado(eq(mentorado.getId()), isNull()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{lido, am}, new Object[]{naoLido1, null}, new Object[]{naoLido2, null}));

        assertThat(service().resumo(usuarioId).naoLidos()).isEqualTo(2);
    }

    @Test
    void marcarLidoCriaLinhaNovaQuandoMentoradoNuncaInteragiu() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID(), Plano.GRATUITO);
        Aviso a = aviso("Aviso", CategoriaAviso.GERAL, Plano.GRATUITO);
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(avisoRepository.findById(a.getId())).thenReturn(Optional.of(a));
        when(avisoMentoradoRepository.findByMentoradoIdAndAvisoId(mentorado.getId(), a.getId())).thenReturn(Optional.empty());
        when(avisoMentoradoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().marcarLido(usuarioId, a.getId());

        verify(avisoMentoradoRepository).save(argThatLido());
    }

    @Test
    void marcarLidoReaproveitaLinhaExistente() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID(), Plano.GRATUITO);
        Aviso a = aviso("Aviso", CategoriaAviso.GERAL, Plano.GRATUITO);
        AvisoMentorado existente = new AvisoMentorado(mentorado, a);
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(avisoRepository.findById(a.getId())).thenReturn(Optional.of(a));
        when(avisoMentoradoRepository.findByMentoradoIdAndAvisoId(mentorado.getId(), a.getId())).thenReturn(Optional.of(existente));
        when(avisoMentoradoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().marcarLido(usuarioId, a.getId());

        assertThat(existente.isLido()).isTrue();
    }

    @Test
    void marcarTodosLidosPulaOsJaLidosESoSalvaOsPendentes() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID(), Plano.GRATUITO);
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));

        Aviso jaLido = aviso("Já lido", CategoriaAviso.GERAL, Plano.GRATUITO);
        Aviso pendente = aviso("Pendente", CategoriaAviso.GERAL, Plano.GRATUITO);
        AvisoMentorado amLido = new AvisoMentorado(mentorado, jaLido);
        amLido.marcarLido();
        when(avisoMentoradoRepository.buscarParaMentorado(eq(mentorado.getId()), isNull()))
                .thenReturn(List.<Object[]>of(new Object[]{jaLido, amLido}, new Object[]{pendente, null}));
        when(avisoMentoradoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().marcarTodosLidos(usuarioId);

        verify(avisoMentoradoRepository, times(1)).save(any());
        verify(avisoMentoradoRepository, never()).save(argThatIs(amLido));
    }

    private static AvisoMentorado argThatLido() {
        return org.mockito.ArgumentMatchers.argThat(AvisoMentorado::isLido);
    }

    private static AvisoMentorado argThatIs(AvisoMentorado expected) {
        return org.mockito.ArgumentMatchers.argThat(actual -> actual == expected);
    }
}
