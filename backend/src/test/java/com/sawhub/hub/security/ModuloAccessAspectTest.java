package com.sawhub.hub.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Modulo;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * O coração do RBAC por área (M4 da revisão de segurança aponta que essa camada fina é
 * opt-in por design — por isso importa MUITO que a decisão em si esteja 100% certa aqui,
 * já que não há uma segunda camada de default-deny por módulo ainda).
 */
class ModuloAccessAspectTest {

    private final ModuloAccessAspect aspect = new ModuloAccessAspect();

    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    private static RequiresModulo requires(Modulo... modulos) {
        return new RequiresModulo() {
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return RequiresModulo.class;
            }

            public Modulo[] value() {
                return modulos;
            }
        };
    }

    private static void autenticarComo(Area area) {
        var principal = new AppUserPrincipal(UUID.randomUUID(), "user@sawhub.com.br", "hash",
                "Usuário Teste", Perfil.ADMIN, area, List.of("ROLE_ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal, null, "ROLE_ADMIN"));
    }

    @Test
    void semAutenticacaoNenhuma_barraComAccessDenied() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> aspect.checarModulo(requires(Modulo.TIME)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void principalSemArea_barraComAccessDenied() {
        var principal = new AppUserPrincipal(UUID.randomUUID(), "mentorado@saborearte.com.br", "hash",
                "Mentorado", Perfil.MENTORADO, null, List.of("ROLE_MENTORADO"));
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal, null, "ROLE_MENTORADO"));

        assertThatThrownBy(() -> aspect.checarModulo(requires(Modulo.MENTORADOS)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void areaComPermissaoPassaSemLancarNada() {
        autenticarComo(Area.COMERCIAL);

        assertThatCode(() -> aspect.checarModulo(requires(Modulo.COMERCIAL))).doesNotThrowAnyException();
    }

    @Test
    void areaSemPermissao_barraComAccessDenied() {
        autenticarComo(Area.COMERCIAL);

        assertThatThrownBy(() -> aspect.checarModulo(requires(Modulo.TIME)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void areaSemPermissao_barraFinanceiroInclusive() {
        // Financeiro (E14) é o próximo módulo de risco alto a entrar na esteira — este teste
        // já fica pronto pra pegar regressão assim que ele existir.
        autenticarComo(Area.GESTAO_PERFORMANCE);

        assertThatThrownBy(() -> aspect.checarModulo(requires(Modulo.FINANCEIRO)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void fundadorPassaEmQualquerModulo() {
        autenticarComo(Area.ADMIN);

        for (Modulo modulo : Modulo.values()) {
            assertThatCode(() -> aspect.checarModulo(requires(modulo))).doesNotThrowAnyException();
        }
    }

    // Achado do Marcos (22/07/2026, MentoradoContratoController) — @RequiresModulo com múltiplos
    // valores é OR: basta a área ter UM dos módulos liberado, não os dois.
    @Test
    void multiplosModulos_passaSeAreaTiverPeloMenosUmDeles() {
        autenticarComo(Area.GESTAO_PERFORMANCE);

        assertThatCode(() -> aspect.checarModulo(requires(Modulo.COMERCIAL, Modulo.MENTORADOS)))
                .doesNotThrowAnyException();
    }

    @Test
    void multiplosModulos_barraSeAreaNaoTiverNenhumDeles() {
        autenticarComo(Area.MARKETING);

        assertThatThrownBy(() -> aspect.checarModulo(requires(Modulo.COMERCIAL, Modulo.MENTORADOS)))
                .isInstanceOf(AccessDeniedException.class);
    }
}
