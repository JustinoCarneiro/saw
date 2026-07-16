package com.sawhub.hub.team;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Fonte única de verdade do RBAC por área (E15/H15.2-H15.5). Login, /me e
 * /team/permission-matrix leem todos daqui — um bug aqui vaza ou barra módulo errado
 * silenciosamente em produção, então cada área é fixada explicitamente, não só o total.
 */
class AreaModuloMatrixTest {

    @Test
    void comercialSoAcessaComercial() {
        assertThat(AreaModuloMatrix.allowedModulos(Area.COMERCIAL)).containsExactly(Modulo.COMERCIAL);
    }

    @Test
    void marketingSoAcessaConteudos() {
        assertThat(AreaModuloMatrix.allowedModulos(Area.MARKETING)).containsExactly(Modulo.CONTEUDOS);
    }

    @Test
    void gestaoDePerformanceAcessaMentoradosConteudosEPainelConsolidado() {
        assertThat(AreaModuloMatrix.allowedModulos(Area.GESTAO_PERFORMANCE))
                .containsExactlyInAnyOrder(Modulo.MENTORADOS, Modulo.CONTEUDOS, Modulo.PAINEL_CONSOLIDADO);
    }

    @Test
    void gestaoDePerformanceNaoAcessaTimeNemFinanceiro() {
        Set<Modulo> permitidos = AreaModuloMatrix.allowedModulos(Area.GESTAO_PERFORMANCE);
        assertThat(permitidos).doesNotContain(Modulo.TIME, Modulo.FINANCEIRO, Modulo.COMERCIAL, Modulo.DASHBOARD);
    }

    @Test
    void fundadorAcessaTodosOsModulosSemExcecao() {
        assertThat(AreaModuloMatrix.allowedModulos(Area.ADMIN)).containsExactlyInAnyOrder(Modulo.values());
    }

    @Test
    void isAllowedReflete_allowedModulos_paraTodaCombinacao() {
        for (Area area : Area.values()) {
            for (Modulo modulo : Modulo.values()) {
                boolean esperado = AreaModuloMatrix.allowedModulos(area).contains(modulo);
                assertThat(AreaModuloMatrix.isAllowed(area, modulo))
                        .as("isAllowed(%s, %s)", area, modulo)
                        .isEqualTo(esperado);
            }
        }
    }

    @Test
    void nenhumaAreaAlemDoFundadorAcessaFinanceiro() {
        for (Area area : Area.values()) {
            if (area == Area.ADMIN) {
                continue;
            }
            assertThat(AreaModuloMatrix.allowedModulos(area))
                    .as("Financeiro é módulo de risco alto (E14) — só Fundador acessa por padrão")
                    .doesNotContain(Modulo.FINANCEIRO);
        }
    }
}
