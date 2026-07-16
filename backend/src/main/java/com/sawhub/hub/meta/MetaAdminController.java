package com.sawhub.hub.meta;

import com.sawhub.hub.common.dto.ImportResultResponse;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Modulo;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** M23 — primeiros endpoints Admin para o módulo de Metas (que até então era 100% self-service
 * do mentorado, M09). O import em massa pelo Admin apoia o onboarding de novos mentorados. */
@RestController
@RequestMapping("/api/v1/admin/metas")
@RequiresModulo(Modulo.MENTORADOS)
public class MetaAdminController {

    private final MetaCsvService metaCsvService;

    public MetaAdminController(MetaCsvService metaCsvService) {
        this.metaCsvService = metaCsvService;
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportarCsv() {
        String csv = metaCsvService.exportar();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"metas.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }

    @PostMapping("/import")
    public ResponseEntity<ImportResultResponse> importarCsv(@RequestParam("arquivo") MultipartFile arquivo) {
        ImportResultResponse response = metaCsvService.importar(arquivo);
        if (!response.erros().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
