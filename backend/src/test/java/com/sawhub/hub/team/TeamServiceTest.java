package com.sawhub.hub.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentoria.Mentoria;
import com.sawhub.hub.mentoria.MentoriaRepository;
import com.sawhub.hub.mentoria.TipoMentoria;
import com.sawhub.hub.security.Perfil;
import com.sawhub.hub.security.Usuario;
import com.sawhub.hub.security.UsuarioRepository;
import com.sawhub.hub.team.dto.ColaboradorResponse;
import com.sawhub.hub.team.dto.CriarColaboradorRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ColaboradorRepository colaboradorRepository;
    @Mock
    private MentoriaRepository mentoriaRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TeamService teamService;

    @Test
    void listarDelegaParaRepositorioOrdenadoPorNome() {
        teamService.listar();
        verify(colaboradorRepository).findAllByOrderByNomeAsc();
    }

    // H15.6 — carteira computada por leitura, nunca armazenada (achado do M20: as colunas antigas
    // só carregavam o valor literal que o seeder escrevia na primeira carga).
    @Test
    void listarComputaCarteiraComoMentoradosDistintosDoMentor() {
        Usuario usuario = new Usuario("lucas@sawhub.com.br", "hash", Perfil.ADMIN);
        Colaborador mentor = new Colaborador(usuario, "Lucas", Area.GESTAO_PERFORMANCE);
        ReflectionTestUtils.setField(mentor, "id", java.util.UUID.randomUUID());
        when(colaboradorRepository.findAllByOrderByNomeAsc()).thenReturn(List.of(mentor));

        Mentorado a = mentorado();
        Mentorado b = mentorado();
        Mentoria individual1 = new Mentoria(TipoMentoria.INDIVIDUAL, mentor, Set.of(a), Instant.now(), 60, null, null);
        Mentoria individual2 = new Mentoria(TipoMentoria.INDIVIDUAL, mentor, Set.of(a), Instant.now(), 60, null, null);
        Mentoria grupo = new Mentoria(TipoMentoria.GRUPO, mentor, Set.of(a, b), Instant.now(), 60, null, null);
        when(mentoriaRepository.buscarPorMentor(mentor)).thenReturn(List.of(individual1, individual2, grupo));

        List<ColaboradorResponse> resultado = teamService.listar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).carteira()).isEqualTo(2L);
    }

    private static Mentorado mentorado() {
        Mentorado m = new Mentorado(null, "Mentorado", "Negócio", com.sawhub.hub.mentorado.Plano.BASICO,
                java.math.BigDecimal.ZERO, 0, 0);
        ReflectionTestUtils.setField(m, "id", java.util.UUID.randomUUID());
        return m;
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
