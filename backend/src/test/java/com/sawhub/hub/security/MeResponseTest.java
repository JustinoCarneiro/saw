package com.sawhub.hub.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.AreaModuloMatrix;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** GET /api/v1/auth/me — o frontend filtra a sidebar inteira a partir de modulosPermitidos. */
class MeResponseTest {

    @Test
    void modulosPermitidosBateComAreaModuloMatrix() {
        var principal = new AppUserPrincipal(UUID.randomUUID(), "lucas@sawhub.com.br", "hash",
                "Lucas Alves", Perfil.ADMIN, Area.GESTAO_PERFORMANCE, List.of("ROLE_ADMIN"));

        var response = MeResponse.from(principal);

        assertThat(response.modulosPermitidos()).containsExactlyInAnyOrderElementsOf(
                AreaModuloMatrix.allowedModulos(Area.GESTAO_PERFORMANCE).stream().map(Enum::name).toList());
    }

    @Test
    void mentoradoSemAreaRecebeListaDeModulosVazia() {
        var principal = new AppUserPrincipal(UUID.randomUUID(), "joao@saborearte.com.br", "hash",
                "João Silva", Perfil.MENTORADO, null, List.of("ROLE_MENTORADO"));

        var response = MeResponse.from(principal);

        assertThat(response.modulosPermitidos()).isEmpty();
        assertThat(response.area()).isNull();
        assertThat(response.perfil()).isEqualTo("MENTORADO");
    }

    @Test
    void naoExpoeSenhaOuHashDeQualquerForma() {
        var principal = new AppUserPrincipal(UUID.randomUUID(), "matheus@sawhub.com.br", "hash-secreto",
                "Matheus Brayan", Perfil.ADMIN, Area.FUNDADOR, List.of("ROLE_ADMIN"));

        var response = MeResponse.from(principal);

        assertThat(response.toString()).doesNotContain("hash-secreto");
    }
}
