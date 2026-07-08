package com.sawhub.hub.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sawhub.hub.security.Perfil;
import com.sawhub.hub.security.Usuario;
import com.sawhub.hub.security.UsuarioRepository;
import com.sawhub.hub.team.dto.CriarColaboradorRequest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ColaboradorRepository colaboradorRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TeamService teamService;

    @Test
    void listarDelegaParaRepositorioOrdenadoPorNome() {
        teamService.listar();
        verify(colaboradorRepository).findAllByOrderByNomeAsc();
    }

    @Test
    void criarRejeitaEmailJaCadastrado() {
        var request = new CriarColaboradorRequest("Novo Colaborador", "existente@sawhub.com.br", "senha1234", Area.COMERCIAL);
        when(usuarioRepository.findByEmail("existente@sawhub.com.br"))
                .thenReturn(Optional.of(new Usuario("existente@sawhub.com.br", "hash", Perfil.ADMIN)));

        assertThatThrownBy(() -> teamService.criar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Já existe uma conta");

        verify(usuarioRepository, never()).save(any());
        verify(colaboradorRepository, never()).save(any());
    }

    @Test
    void criarPersisteUsuarioAdminComSenhaCodificadaEColaboradorNaAreaPedida() {
        var request = new CriarColaboradorRequest("Nova Pessoa", "nova@sawhub.com.br", "senha1234", Area.MARKETING);
        when(usuarioRepository.findByEmail("nova@sawhub.com.br")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("senha1234")).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(colaboradorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Colaborador criado = teamService.criar(request);

        assertThat(criado.getNome()).isEqualTo("Nova Pessoa");
        assertThat(criado.getArea()).isEqualTo(Area.MARKETING);
        assertThat(criado.getUsuario().getEmail()).isEqualTo("nova@sawhub.com.br");
        assertThat(criado.getUsuario().getPasswordHash()).isEqualTo("hash-bcrypt");
        assertThat(criado.getUsuario().getPerfil()).isEqualTo(Perfil.ADMIN);
    }
}
