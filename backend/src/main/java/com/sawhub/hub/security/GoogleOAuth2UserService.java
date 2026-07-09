package com.sawhub.hub.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/** H1.2 (M07) — login com Google. Contas nascem por ação do Admin (E11), não por auto-cadastro:
 * "vincular" aqui é casar pelo e-mail com um {@link Usuario} que já existe, nunca criar um novo.
 * Reaproveita {@link CustomUserDetailsService} tal qual — mesmo cálculo de authorities pra ADMIN
 * e MENTORADO usado no login e-mail/senha, zero lógica duplicada. */
@Service
public class GoogleOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final CustomUserDetailsService customUserDetailsService;
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

    // @Autowired explícito de propósito: com 2 construtores na classe, o Spring não infere
    // sozinho qual usar (achado ao vivo — sem isto, o boot falhava tentando um construtor sem
    // argumentos que não existe, mesmo havendo só um construtor público).
    @Autowired
    public GoogleOAuth2UserService(CustomUserDetailsService customUserDetailsService) {
        this(customUserDetailsService, new DefaultOAuth2UserService());
    }

    // Composição (não herança de DefaultOAuth2UserService) de propósito: o delegate faz uma
    // chamada HTTP de verdade pro userinfo endpoint do Google — sem isolar atrás de uma
    // interface, não dava pra testar a lógica de verificação/rejeição sem um servidor real.
    GoogleOAuth2UserService(CustomUserDetailsService customUserDetailsService,
                             OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate) {
        this.customUserDetailsService = customUserDetailsService;
        this.delegate = delegate;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User googleUser = delegate.loadUser(userRequest);

        // Um provedor OAuth2 mal-configurado (ou malicioso, num cenário de múltiplos provedores)
        // podia devolver um e-mail não verificado — não é hipotético o suficiente pra pular esta
        // checagem num módulo de risco alto (CLAUDE.md: Auth entra sempre com revisor-seguranca).
        Boolean emailVerificado = googleUser.getAttribute("email_verified");
        String email = googleUser.getAttribute("email");
        if (email == null || emailVerificado == null || !emailVerificado) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_nao_verificado"), "E-mail do Google não verificado.");
        }

        try {
            return (AppUserPrincipal) customUserDetailsService.loadUserByUsername(email);
        } catch (UsernameNotFoundException e) {
            // Código e mensagem propositalmente genéricos (H1.1, mesmo princípio que
            // AuthFailureHandler/CustomUserDetailsService já aplicam no login e-mail/senha) —
            // achado do revisor-seguranca: um código "conta_nao_encontrada" seria um oráculo de
            // enumeração de contas (confirma pra quem autentica no Google se aquele e-mail tem
            // conta SAW HUB). O link "Solicitar acesso" já fica sempre visível na tela de login,
            // não depende do backend confirmar o motivo exato da rejeição.
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("login_nao_permitido"),
                    "Não foi possível concluir o login com esta conta Google.");
        }
    }
}
