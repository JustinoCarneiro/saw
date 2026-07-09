package com.sawhub.hub.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sawhub.hub.team.Area;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Principal próprio guardado na sessão — nunca a entidade JPA crua (o proxy do Hibernate
 * não serializa de forma confiável no Redis). Só primitivos/enums/UUID aqui.
 *
 * Imutável e sem construtor padrão, então o Jackson usado pelo GenericJackson2JsonRedisSerializer
 * não consegue reconstruí-la via bean introspection sozinho — precisa de @JsonCreator explícito
 * (mesmo motivo pelo qual as próprias classes do Spring Security, como
 * UsernamePasswordAuthenticationToken, exigem os mixins de SecurityJackson2Modules).
 * getAuthorities()/getPassword()/getUsername() (exigidos pela interface UserDetails) ficam
 * @JsonIgnore porque colidem em nome/tipo com os getters "reais" usados pelo Jackson.
 *
 * Implementa também {@link OAuth2User} (M07 — Google OAuth): o mesmo principal serve pros dois
 * fluxos de login (e-mail/senha via {@code DaoAuthenticationProvider} e Google via
 * {@code oauth2Login()}), então todo o resto do app (ModuloAccessAspect, @AuthenticationPrincipal
 * AppUserPrincipal nos controllers) funciona igual não importa por qual caminho a sessão nasceu.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AppUserPrincipal implements UserDetails, OAuth2User {

    private final UUID usuarioId;
    private final String email;
    private final String passwordHash;
    private final String nome;
    private final Perfil perfil;
    private final Area area;
    private final List<String> authorityStrings;

    @JsonCreator
    public AppUserPrincipal(@JsonProperty("usuarioId") UUID usuarioId,
                             @JsonProperty("email") String email,
                             @JsonProperty("passwordHash") String passwordHash,
                             @JsonProperty("nome") String nome,
                             @JsonProperty("perfil") Perfil perfil,
                             @JsonProperty("area") Area area,
                             @JsonProperty("authorityStrings") List<String> authorityStrings) {
        this.usuarioId = usuarioId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.nome = nome;
        this.perfil = perfil;
        this.area = area;
        // Cópia defensiva pra ArrayList de propósito: a allowlist do
        // SecurityJackson2Modules.enableDefaultTyping() (achado M3) reconhece ArrayList, mas NÃO
        // reconhece as classes internas imutáveis que List.of()/Collectors.toList() podem
        // devolver (ex.: java.util.ImmutableCollections$List12) — desserializar a sessão
        // quebraria com "classe não está na allowlist". Hoje todo caller já passa ArrayList,
        // mas isto blinda contra alguém trocar por List.of(...) no futuro sem perceber o problema.
        this.authorityStrings = authorityStrings == null ? null : new ArrayList<>(authorityStrings);
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getEmail() {
        return email;
    }

    // @JsonIgnore de propósito (achado M2 da revisão de segurança): o hash BCrypt não precisa
    // viajar pra dentro da sessão do Redis — depois da autenticação inicial (DaoAuthenticationProvider)
    // ninguém mais lê getPassword()/getPasswordHash() do principal já autenticado. Guardá-lo lá só
    // amplia à toa a superfície de quem consegue ler o Redis. O construtor aceita null aqui na
    // desserialização (o campo não é reconstituído, e não faz falta).
    @JsonIgnore
    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNome() {
        return nome;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public Area getArea() {
        return area;
    }

    public List<String> getAuthorityStrings() {
        return authorityStrings;
    }

    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorityStrings.stream().map(SimpleGrantedAuthority::new).toList();
    }

    @JsonIgnore
    @Override
    public String getPassword() {
        return passwordHash;
    }

    @JsonIgnore
    @Override
    public String getUsername() {
        return email;
    }

    // OAuth2User (M07): não guardamos os atributos crus do Google na sessão — depois do login
    // ninguém no app lê getAttributes() do principal já autenticado (mesmo raciocínio do
    // getPasswordHash() acima), e evita carregar PII do Google (nome/foto/locale) no Redis à toa.
    @JsonIgnore
    @Override
    public Map<String, Object> getAttributes() {
        return Map.of();
    }

    // AuthenticatedPrincipal#getName() — usado por Spring Security internamente (ex.: logs de
    // auditoria), não pelo resto do app (que já usa getEmail()/getUsuarioId()).
    @JsonIgnore
    @Override
    public String getName() {
        return email;
    }
}
