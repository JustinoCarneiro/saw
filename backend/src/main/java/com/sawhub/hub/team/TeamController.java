package com.sawhub.hub.team;

import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.dto.ColaboradorResponse;
import com.sawhub.hub.team.dto.CriarColaboradorRequest;
import com.sawhub.hub.team.dto.PermissionMatrixRow;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/team")
@RequiresModulo(Modulo.TIME)
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    public List<ColaboradorResponse> listar() {
        return teamService.listar().stream().map(ColaboradorResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ColaboradorResponse criar(@Valid @RequestBody CriarColaboradorRequest request) {
        return ColaboradorResponse.from(teamService.criar(request));
    }

    @GetMapping("/permission-matrix")
    public List<PermissionMatrixRow> matrizDePermissoes() {
        return PermissionMatrixRow.full();
    }
}
