package com.sawhub.hub.aviso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.sawhub.hub.aviso.dto.CriarAvisoRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** H12.2 — RED primeiro: AvisoAdminService ainda não existia neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class AvisoAdminServiceTest {

    @Mock
    private AvisoRepository avisoRepository;

    @Mock
    private AvisoMentoradoRepository avisoMentoradoRepository;

    private AvisoAdminService service() {
        return new AvisoAdminService(avisoRepository, avisoMentoradoRepository);
    }

    @Test
    void criarJaPublicaImediatamenteSemEstadoDeRascunho() {
        when(avisoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new CriarAvisoRequest("Nova mentoria em grupo", "Inscrições abertas.",
                CategoriaAviso.MENTORIAS);
        Aviso criado = service().criar(request);

        assertThat(criado.getTitulo()).isEqualTo("Nova mentoria em grupo");
        assertThat(criado.getCategoria()).isEqualTo(CategoriaAviso.MENTORIAS);

        ArgumentCaptor<Aviso> captor = ArgumentCaptor.forClass(Aviso.class);
        verify(avisoRepository).save(captor.capture());
        assertThat(captor.getValue().getTitulo()).isEqualTo("Nova mentoria em grupo");
    }

    @Test
    void listarDelegaParaRepositorioOrdenadoPorMaisRecente() {
        Aviso a = new Aviso("Título", "Desc", CategoriaAviso.GERAL);
        when(avisoRepository.findAllByOrderByCriadoEmDesc()).thenReturn(List.of(a));

        assertThat(service().listar()).containsExactly(a);
    }

    @Test
    void atualizarAlteraCamposDoAvisoExistente() {
        UUID id = UUID.randomUUID();
        Aviso existente = new Aviso("Antigo", "Desc antiga", CategoriaAviso.GERAL);
        when(avisoRepository.findById(id)).thenReturn(Optional.of(existente));

        var request = new CriarAvisoRequest("Novo título", "Nova descrição", CategoriaAviso.EVENTOS);
        Aviso atualizado = service().atualizar(id, request);

        assertThat(atualizado.getTitulo()).isEqualTo("Novo título");
        assertThat(atualizado.getDescricao()).isEqualTo("Nova descrição");
        assertThat(atualizado.getCategoria()).isEqualTo(CategoriaAviso.EVENTOS);
    }

    @Test
    void atualizarComIdInexistenteLancaErro() {
        UUID id = UUID.randomUUID();
        when(avisoRepository.findById(id)).thenReturn(Optional.empty());

        var request = new CriarAvisoRequest("Título", "Desc", CategoriaAviso.GERAL);
        assertThatThrownBy(() -> service().atualizar(id, request)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void excluirRemoveLeiturasAntesDoAvisoParaNaoQuebrarAFk() {
        UUID id = UUID.randomUUID();
        when(avisoRepository.existsById(id)).thenReturn(true);

        service().excluir(id);

        verify(avisoMentoradoRepository).deleteAllByAvisoId(id);
        verify(avisoRepository).deleteById(id);
    }

    @Test
    void excluirComIdInexistenteLancaErroSemTocarNoRepositorio() {
        UUID id = UUID.randomUUID();
        when(avisoRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service().excluir(id)).isInstanceOf(IllegalArgumentException.class);

        verify(avisoMentoradoRepository, never()).deleteAllByAvisoId(any());
        verify(avisoRepository, never()).deleteById(any());
    }
}
