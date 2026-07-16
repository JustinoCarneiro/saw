package com.sawhub.hub.team;

import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.dto.ColaboradorResponse;
import com.sawhub.hub.team.dto.CriarColaboradorRequest;
import com.sawhub.hub.team.dto.DesempenhoColaboradorResponse;
import com.sawhub.hub.team.dto.PermissionMatrixRow;
import com.sawhub.hub.common.dto.ImportResultResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/v1/admin/team")
@RequiresModulo(Modulo.TIME)
@Validated
public class TeamController {

    private final TeamService teamService;
    private final DesempenhoTimeService desempenhoTimeService;
    private final TeamCsvService teamCsvService;

    public TeamController(TeamService teamService, DesempenhoTimeService desempenhoTimeService,
                           TeamCsvService teamCsvService) {
        this.teamService = teamService;
        this.desempenhoTimeService = desempenhoTimeService;
        this.teamCsvService = teamCsvService;
    }

    @GetMapping
    public List<ColaboradorResponse> listar() {
        return teamService.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ColaboradorResponse criar(@Valid @RequestBody CriarColaboradorRequest request) {
        Colaborador criado = teamService.criar(request);
        return ColaboradorResponse.from(criado, teamService.carteiraDe(criado));
    }

    @GetMapping("/permission-matrix")
    public List<PermissionMatrixRow> matrizDePermissoes() {
        return PermissionMatrixRow.full();
    }

    // H15.7 (M20)
    @GetMapping("/desempenho")
    public List<DesempenhoColaboradorResponse> desempenho(@RequestParam @Min(2020) int ano,
                                                            @RequestParam @Min(1) @Max(12) int mes) {
        return desempenhoTimeService.desempenho(ano, mes);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportarCsv() {
        String csv = teamCsvService.exportar();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"colaboradores.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }

    @PostMapping("/import")
    public ResponseEntity<ImportResultResponse> importarCsv(@RequestParam("arquivo") MultipartFile arquivo) {
        ImportResultResponse response = teamCsvService.importar(arquivo);
        if (!response.erros().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
