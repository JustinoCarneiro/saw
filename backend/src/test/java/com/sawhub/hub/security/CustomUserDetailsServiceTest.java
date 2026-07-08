package com.sawhub.hub.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.mentorado.Plano;
import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
import com.sawhub.hub.team.ColaboradorRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ColaboradorRepository colaboradorRepository;
    @Mock
    private MentoradoRepository mentoradoRepository;

    private CustomUserDetailsService service() {
        return new CustomUserDetailsService(usuarioRepository, colaboradorRepository, mentoradoRepository);
    }

    @Test
    void emailInexistenteLancaUsernameNotFound() {
        when(usuarioRepository.findByEmail("ninguem@sawhub.com.br")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().loadUserByUsername("ninguem@sawhub.com.br"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void adminMontaPrincipalComRoleAdminEModulosDaSuaArea() {
        Usuario usuario = new Usuario("paula@sawhub.com.br", "hash", Perfil.ADMIN);
        Colaborador colaborador = new Colaborador(usuario, "Paula Mendes", Area.COMERCIAL, null, new BigDecimal("22.5"));
        when(usuarioRepository.findByEmail("paula@sawhub.com.br")).thenReturn(Optional.of(usuario));
        when(colaboradorRepository.findByUsuario(usuario)).thenReturn(Optional.of(colaborador));

        var principal = (AppUserPrincipal) service().loadUserByUsername("paula@sawhub.com.br");

        assertThat(principal.getNome()).isEqualTo("Paula Mendes");
        assertThat(principal.getArea()).isEqualTo(Area.COMERCIAL);
        assertThat(principal.getAuthorityStrings()).contains("ROLE_ADMIN", "MODULO_COMERCIAL");
        assertThat(principal.getAuthorityStrings()).doesNotContain("MODULO_TIME", "MODULO_FINANCEIRO");
    }

    @Test
    void mentoradoMontaPrincipalComRoleMentoradoESemArea() {
        Usuario usuario = new Usuario("joao@saborearte.com.br", "hash", Perfil.MENTORADO);
        Mentorado mentorado = new Mentorado(usuario, "João Silva", "Restaurante Sabor & Arte", Plano.PROFISSIONAL,
                new BigDecimal("18.0"), 3, 3);
        when(usuarioRepository.findByEmail("joao@saborearte.com.br")).thenReturn(Optional.of(usuario));
        when(mentoradoRepository.findByUsuario(usuario)).thenReturn(Optional.of(mentorado));

        var principal = (AppUserPrincipal) service().loadUserByUsername("joao@saborearte.com.br");

        assertThat(principal.getNome()).isEqualTo("João Silva");
        assertThat(principal.getArea()).isNull();
        assertThat(principal.getAuthorityStrings()).containsExactly("ROLE_MENTORADO");
    }

    @Test
    void adminSemColaboradorAssociadoLancaUsernameNotFound_naoQuebraComOutroErro() {
        // Inconsistência de dados (usuário ADMIN sem linha em colaborador) não pode vazar
        // como NullPointerException/500 genérico — tem que ser tratada como credencial inválida.
        Usuario usuario = new Usuario("orfao@sawhub.com.br", "hash", Perfil.ADMIN);
        when(usuarioRepository.findByEmail("orfao@sawhub.com.br")).thenReturn(Optional.of(usuario));
        when(colaboradorRepository.findByUsuario(usuario)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().loadUserByUsername("orfao@sawhub.com.br"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
