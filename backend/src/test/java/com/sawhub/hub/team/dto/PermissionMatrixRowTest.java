package com.sawhub.hub.team.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.AreaModuloMatrix;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * A tela de Time (frontend) renderiza a matriz inteira a partir daqui — precisa ser um
 * espelho fiel de AreaModuloMatrix, nunca uma cópia que possa divergir dela.
 */
class PermissionMatrixRowTest {

    @Test
    void temExatamenteUmaLinhaPorArea() {
        List<PermissionMatrixRow> full = PermissionMatrixRow.full();
        assertThat(full).hasSize(Area.values().length);
        assertThat(full.stream().map(PermissionMatrixRow::area)).containsExactlyInAnyOrder(
                "COMERCIAL", "MARKETING", "GESTAO_PERFORMANCE", "FUNDADOR");
    }

    @Test
    void cadaLinhaBateComAreaModuloMatrix() {
        for (PermissionMatrixRow row : PermissionMatrixRow.full()) {
            Area area = Area.valueOf(row.area());
            List<String> esperado = AreaModuloMatrix.allowedModulos(area).stream().map(Enum::name).toList();
            assertThat(row.modulosPermitidos()).containsExactlyInAnyOrderElementsOf(esperado);
        }
    }
}
