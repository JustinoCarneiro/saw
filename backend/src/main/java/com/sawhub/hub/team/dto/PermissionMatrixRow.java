package com.sawhub.hub.team.dto;

import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.AreaModuloMatrix;
import java.util.List;

public record PermissionMatrixRow(String area, List<String> modulosPermitidos) {

    public static List<PermissionMatrixRow> full() {
        return java.util.Arrays.stream(Area.values())
                .map(area -> new PermissionMatrixRow(
                        area.name(),
                        AreaModuloMatrix.allowedModulos(area).stream().map(Enum::name).toList()))
                .toList();
    }
}
