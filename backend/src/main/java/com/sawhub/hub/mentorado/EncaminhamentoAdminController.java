package com.sawhub.hub.mentorado;

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

/** M23 — primeiros endpoints Admin de CRUD direto para Encaminhamento (o painel consolidado no M17
 * só via dados agregados; a criação/edição era 100% self-service do mentorado ou gerada via
 * publicação de ata). */
@RestController
@RequestMapping("/api/v1/admin/encaminhamentos")
@RequiresModulo(Modulo.MENTORADOS)
public class EncaminhamentoAdminController {

    private final EncaminhamentoCsvService encaminhamentoCsvService;

    public EncaminhamentoAdminController(EncaminhamentoCsvService encaminhamentoCsvService) {
        this.encaminhamentoCsvService = encaminhamentoCsvService;
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportarCsv() {
        String csv = encaminhamentoCsvService.exportar();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tarefas.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }

    @PostMapping("/import")
    public ResponseEntity<ImportResultResponse> importarCsv(@RequestParam("arquivo") MultipartFile arquivo) {
        ImportResultResponse response = encaminhamentoCsvService.importar(arquivo);
        if (!response.erros().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
