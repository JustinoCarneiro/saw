package com.sawhub.hub.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sawhub.hub.team.Area;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.jackson2.SecurityJackson2Modules;

/**
 * AppUserPrincipal é o que fica guardado na sessão do Redis, serializado em JSON. Isto já
 * quebrou de verdade nesta mesma sessão de desenvolvimento (faltava @JsonCreator, depois
 * faltavam os mixins de SecurityJackson2Modules) — este teste replica exatamente o
 * ObjectMapper de RedisSessionConfig E a forma real de armazenamento (o principal nunca é
 * serializado sozinho — sempre aninhado dentro de SecurityContextImpl.authentication.principal,
 * que é onde o Spring Session realmente o guarda).
 *
 * Serializar o principal SOZINHO (bare root value) NÃO funciona com este ObjectMapper —
 * SecurityJackson2Modules.enableDefaultTyping() só embute o discriminador "@class" pra tipos
 * aninhados em campo de tipo declarado diferente (Object/Authentication), não pro valor raiz.
 * Isso é irrelevante na prática porque o principal nunca é serializado sozinho de verdade.
 */
class AppUserPrincipalTest {

    private static ObjectMapper redisLikeMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(SecurityJackson2Modules.getModules(AppUserPrincipalTest.class.getClassLoader()));
        SecurityJackson2Modules.enableDefaultTyping(mapper);
        return mapper;
    }

    private static AppUserPrincipal fundador() {
        return new AppUserPrincipal(UUID.randomUUID(), "matheus@sawhub.com.br", "hash-bcrypt-secreto",
                "Matheus Brayan", Perfil.ADMIN, Area.ADMIN, List.of("ROLE_ADMIN", "MODULO_TIME"));
    }

    private static SecurityContext contextoCom(AppUserPrincipal principal) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return new SecurityContextImpl(authentication);
    }

    @Test
    void getAuthoritiesMapeiaAsStringsParaGrantedAuthority() {
        AppUserPrincipal principal = fundador();

        List<String> authorities = principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        assertThat(authorities).containsExactlyInAnyOrder("ROLE_ADMIN", "MODULO_TIME");
    }

    @Test
    void sobreviveAoRoundtripDentroDoSecurityContextComoOSpringSessionRealmenteGuarda() throws Exception {
        AppUserPrincipal original = fundador();
        ObjectMapper mapper = redisLikeMapper();

        String json = mapper.writeValueAsString(contextoCom(original));
        SecurityContext restaurado = (SecurityContext) mapper.readValue(json, Object.class);
        AppUserPrincipal principal = (AppUserPrincipal) restaurado.getAuthentication().getPrincipal();

        assertThat(principal.getUsuarioId()).isEqualTo(original.getUsuarioId());
        assertThat(principal.getUsername()).isEqualTo(original.getUsername());
        assertThat(principal.getNome()).isEqualTo(original.getNome());
        assertThat(principal.getPerfil()).isEqualTo(original.getPerfil());
        assertThat(principal.getArea()).isEqualTo(original.getArea());
        assertThat(principal.getAuthorities()).hasSameSizeAs(original.getAuthorities());
    }

    @Test
    void hashDeSenhaNaoSobreviveAoRoundtrip() throws Exception {
        // Achado M2 da revisão de segurança: @JsonIgnore de propósito no getPasswordHash().
        // Fixa esse comportamento como intencional — se algum dia isso vazar de volta na
        // sessão serializada, este teste tem que quebrar.
        AppUserPrincipal original = fundador();
        ObjectMapper mapper = redisLikeMapper();

        String json = mapper.writeValueAsString(contextoCom(original));
        SecurityContext restaurado = (SecurityContext) mapper.readValue(json, Object.class);
        AppUserPrincipal principal = (AppUserPrincipal) restaurado.getAuthentication().getPrincipal();

        assertThat(json).doesNotContain("hash-bcrypt-secreto");
        assertThat(principal.getPasswordHash()).isNull();
        assertThat(principal.getPassword()).isNull();
    }

    @Test
    void mentoradoSemAreaSerializaEDesserializaComAreaNula() throws Exception {
        AppUserPrincipal mentorado = new AppUserPrincipal(UUID.randomUUID(), "joao@saborearte.com.br",
                "hash", "João Silva", Perfil.MENTORADO, null, List.of("ROLE_MENTORADO"));
        ObjectMapper mapper = redisLikeMapper();

        String json = mapper.writeValueAsString(contextoCom(mentorado));
        SecurityContext restaurado = (SecurityContext) mapper.readValue(json, Object.class);
        AppUserPrincipal principal = (AppUserPrincipal) restaurado.getAuthentication().getPrincipal();

        assertThat(principal.getArea()).isNull();
        assertThat(principal.getPerfil()).isEqualTo(Perfil.MENTORADO);
    }
}
