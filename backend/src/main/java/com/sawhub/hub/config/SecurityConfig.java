package com.sawhub.hub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sawhub.hub.comercial.LeadRateLimitFilter;
import com.sawhub.hub.mentoria.AtaAudioRateLimitFilter;
import com.sawhub.hub.security.AuthFailureHandler;
import com.sawhub.hub.security.AuthSuccessHandler;
import com.sawhub.hub.security.GoogleOAuth2UserService;
import com.sawhub.hub.security.GoogleOAuthProperties;
import com.sawhub.hub.security.JsonAuthEntryPoint;
import com.sawhub.hub.security.JsonLoginFilter;
import com.sawhub.hub.security.OAuth2FailureHandler;
import com.sawhub.hub.security.OAuth2SuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthSuccessHandler authSuccessHandler;
    private final AuthFailureHandler authFailureHandler;
    private final JsonAuthEntryPoint jsonAuthEntryPoint;
    private final CsrfCookieFilter csrfCookieFilter;
    private final LeadRateLimitFilter leadRateLimitFilter;
    private final AtaAudioRateLimitFilter ataAudioRateLimitFilter;
    private final GoogleOAuth2UserService googleOAuth2UserService;
    private final OAuth2SuccessHandler oauth2SuccessHandler;
    private final OAuth2FailureHandler oauth2FailureHandler;
    private final GoogleOAuthProperties googleOAuthProperties;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${sawhub.cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(AuthSuccessHandler authSuccessHandler, AuthFailureHandler authFailureHandler,
                           JsonAuthEntryPoint jsonAuthEntryPoint, CsrfCookieFilter csrfCookieFilter,
                           LeadRateLimitFilter leadRateLimitFilter, AtaAudioRateLimitFilter ataAudioRateLimitFilter,
                           GoogleOAuth2UserService googleOAuth2UserService, OAuth2SuccessHandler oauth2SuccessHandler,
                           OAuth2FailureHandler oauth2FailureHandler, GoogleOAuthProperties googleOAuthProperties,
                           ObjectMapper objectMapper) {
        this.authSuccessHandler = authSuccessHandler;
        this.authFailureHandler = authFailureHandler;
        this.jsonAuthEntryPoint = jsonAuthEntryPoint;
        this.csrfCookieFilter = csrfCookieFilter;
        this.leadRateLimitFilter = leadRateLimitFilter;
        this.ataAudioRateLimitFilter = ataAudioRateLimitFilter;
        this.googleOAuth2UserService = googleOAuth2UserService;
        this.oauth2SuccessHandler = oauth2SuccessHandler;
        this.oauth2FailureHandler = oauth2FailureHandler;
        this.googleOAuthProperties = googleOAuthProperties;
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        // JsonLoginFilter é instanciado manualmente (fora do DSL .formLogin()), então não herda
        // automaticamente o SecurityContextRepository configurado por http.securityContext(...) —
        // sem isto, o padrão (RequestAttributeSecurityContextRepository) nunca persiste na sessão
        // e a autenticação "funciona" na resposta mas não sobrevive à próxima requisição.
        SecurityContextRepository securityContextRepository = new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository());

        JsonLoginFilter jsonLoginFilter = new JsonLoginFilter(authenticationManager, objectMapper);
        jsonLoginFilter.setAuthenticationSuccessHandler(authSuccessHandler);
        jsonLoginFilter.setAuthenticationFailureHandler(authFailureHandler);
        jsonLoginFilter.setSecurityContextRepository(securityContextRepository);
        // Instanciado fora do DSL .formLogin(), então também não herda o
        // ChangeSessionIdAuthenticationStrategy que o Spring injeta por padrão — sem isto o ID
        // de sessão não é rotacionado no login (proteção contra session fixation, OWASP ASVS 3.2.1).
        jsonLoginFilter.setSessionAuthenticationStrategy(new ChangeSessionIdAuthenticationStrategy());

        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();

        http
                .securityContext(sc -> sc.securityContextRepository(securityContextRepository))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(csrfHandler)
                        // /leads (H1.3, "Solicitar acesso") e o webhook do Mercado Pago (M14) são os únicos
                        // endpoints públicos além do login — quem chama nunca tem sessão prévia pra ter
                        // recebido o cookie XSRF-TOKEN. O webhook é protegido por verificação de
                        // assinatura HMAC própria (MercadoPagoGatewayService.verificarAssinatura), não CSRF.
                        .ignoringRequestMatchers("/api/v1/auth/login", "/api/v1/leads", "/api/v1/webhooks/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/auth/oauth2-config").permitAll()
                        .requestMatchers("/api/v1/leads").permitAll()
                        .requestMatchers("/api/v1/webhooks/**").permitAll()
                        // /oauth2/** e /login/oauth2/** só existem de fato quando
                        // googleOAuthProperties.isEnabled() adiciona .oauth2Login() abaixo —
                        // permitAll aqui não abre nada sozinho.
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        // Achado M4 da revisão de segurança: @RequiresModulo é opt-in (fail-open se
                        // algum controller futuro esquecer a anotação). Financeiro é dado sensível
                        // (E14, risco alto) — por isso ganha default-deny explícito aqui, no filter
                        // chain, além da checagem fina do ModuloAccessAspect. Duas camadas, não uma só.
                        .requestMatchers("/api/v1/admin/financeiro/**").hasAuthority("MODULO_FINANCEIRO")
                        // Achado M2 da revisão de segurança do E13: /admin/comercial/dashboard reexpõe
                        // mrr/vendasLoja lendo do Financeiro (mesma classe de dado do M4 acima) através
                        // de um caminho hoje protegido só pelo @RequiresModulo opt-in — se um refactor
                        // futuro esquecer a anotação, Marketing/Gestão de Performance (que são ROLE_ADMIN
                        // mas não têm MODULO_FINANCEIRO nem MODULO_COMERCIAL) passariam a enxergar MRR e
                        // receita da loja. Mesmo tratamento do Financeiro, mesma razão concreta.
                        .requestMatchers("/api/v1/admin/comercial/**").hasAuthority("MODULO_COMERCIAL")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/mentorado/**").hasRole("MENTORADO")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(jsonAuthEntryPoint))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("SAWHUB_SESSION")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT)))
                .addFilterAt(jsonLoginFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(csrfCookieFilter, org.springframework.security.web.csrf.CsrfFilter.class)
                // Achado M1 da revisão de segurança do E13 — roda antes de qualquer filtro de
                // autenticação, já que /leads nunca autentica (rejeita cedo, sem custo extra).
                .addFilterBefore(leadRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                // Achado (médio) da revisão de segurança do M06 — roda depois (não antes) do filtro
                // de login: precisa do SecurityContextHolder já populado (via sessão, carregado bem
                // mais cedo no chain) pra identificar o usuário autenticado, diferente do leadRateLimitFilter.
                .addFilterAfter(ataAudioRateLimitFilter, UsernamePasswordAuthenticationFilter.class);

        // M07 — só entra no filter chain com credencial configurada. Declarar
        // spring.security.oauth2.client.registration.google.client-id vazio no application.yml
        // faz o Spring Boot falhar o boot (ClientRegistration.validate() exige client-id
        // não-vazio assim que a propriedade existe) — por isso o ClientRegistrationRepository é
        // montado programaticamente aqui dentro do if, nunca via propriedade declarada.
        if (googleOAuthProperties.isEnabled()) {
            http.oauth2Login(oauth2 -> oauth2
                    .clientRegistrationRepository(googleClientRegistrationRepository())
                    .userInfoEndpoint(u -> u.userService(googleOAuth2UserService))
                    .successHandler(oauth2SuccessHandler)
                    .failureHandler(oauth2FailureHandler));
        }

        return http.build();
    }

    private ClientRegistrationRepository googleClientRegistrationRepository() {
        ClientRegistration google = CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(googleOAuthProperties.getClientId())
                .clientSecret(googleOAuthProperties.getClientSecret())
                .build();
        return new InMemoryClientRegistrationRepository(google);
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
