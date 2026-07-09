package com.sawhub.hub.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

/** H1.2 — RED primeiro: GoogleOAuth2UserService ainda não existe neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class GoogleOAuth2UserServiceTest {

    @Mock
    private CustomUserDetailsService customUserDetailsService;
    @Mock
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;
    @Mock
    private OAuth2UserRequest userRequest;

    private GoogleOAuth2UserService service() {
        return new GoogleOAuth2UserService(customUserDetailsService, delegate);
    }

    private static OAuth2User googleUser(String email, Boolean emailVerified) {
        Map<String, Object> attrs = new java.util.HashMap<>();
        attrs.put("sub", "google-subject-id");
        if (email != null) {
            attrs.put("email", email);
        }
        if (emailVerified != null) {
            attrs.put("email_verified", emailVerified);
        }
        return new DefaultOAuth2User(java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")),
                attrs, "sub");
    }

    @Test
    void emailVerificadoComContaExistenteAutenticaComOMesmoPrincipalDoLoginEmailSenha() {
        when(delegate.loadUser(userRequest)).thenReturn(googleUser("maria@restaurante.com", true));
        AppUserPrincipal principal = new AppUserPrincipal(
                java.util.UUID.randomUUID(), "maria@restaurante.com", "hash", "Maria", Perfil.MENTORADO, null,
                new java.util.ArrayList<>(java.util.List.of("ROLE_MENTORADO")));
        when(customUserDetailsService.loadUserByUsername("maria@restaurante.com")).thenReturn(principal);

        OAuth2User resultado = service().loadUser(userRequest);

        assertThat(resultado).isSameAs(principal);
    }

    @Test
    void emailNaoVerificadoLancaOAuth2AuthenticationException() {
        when(delegate.loadUser(userRequest)).thenReturn(googleUser("maria@restaurante.com", false));

        assertThatThrownBy(() -> service().loadUser(userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("não verificado");
    }

    @Test
    void semAtributoEmailVerifiedLancaErro() {
        when(delegate.loadUser(userRequest)).thenReturn(googleUser("maria@restaurante.com", null));

        assertThatThrownBy(() -> service().loadUser(userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void emailSemContaCorrespondenteLancaErroComCodigoGenericoSemConfirmarInexistenciaDeConta() {
        when(delegate.loadUser(userRequest)).thenReturn(googleUser("desconhecido@example.com", true));
        when(customUserDetailsService.loadUserByUsername("desconhecido@example.com"))
                .thenThrow(new UsernameNotFoundException("Credenciais inválidas"));

        // Achado do revisor-seguranca: um código tipo "conta_nao_encontrada" seria um oráculo de
        // enumeração de contas — qualquer um que autentique no Google descobriria se aquele
        // e-mail tem conta SAW HUB. Código e mensagem têm que ser genéricos (mesmo princípio do
        // H1.1 já aplicado no login e-mail/senha via AuthFailureHandler).
        assertThatThrownBy(() -> service().loadUser(userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(ex -> assertThat(((OAuth2AuthenticationException) ex).getError().getErrorCode())
                        .isEqualTo("login_nao_permitido"))
                .hasMessageContaining("Não foi possível concluir o login")
                .hasMessageNotContaining("existe")
                .hasMessageNotContaining("Solicite");
    }
}
