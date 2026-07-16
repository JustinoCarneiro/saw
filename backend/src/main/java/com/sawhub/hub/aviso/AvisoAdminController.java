package com.sawhub.hub.aviso;

import com.sawhub.hub.aviso.dto.AvisoResponse;
import com.sawhub.hub.aviso.dto.CriarAvisoRequest;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Modulo;
import com.sawhub.hub.common.dto.ImportResultResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** H12.2 — gated por {@code Modulo.CONTEUDOS} (Suposição 5 do Blueprint M17: "publicar aviso" é,
 * na prática, a mesma ação de "publicar conteúdo" — mesma área RBAC, sem módulo novo). */
@RestController
@RequestMapping("/api/v1/admin/avisos")
@RequiresModulo(Modulo.CONTEUDOS)
public class AvisoAdminController {

    private final AvisoAdminService avisoAdminService;
    private final AvisoCsvService avisoCsvService;

    public AvisoAdminController(AvisoAdminService avisoAdminService, AvisoCsvService avisoCsvService) {
        this.avisoAdminService = avisoAdminService;
        this.avisoCsvService = avisoCsvService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AvisoResponse criar(@Valid @RequestBody CriarAvisoRequest request) {
        return AvisoResponse.from(avisoAdminService.criar(request));
    }

    @GetMapping
    public List<AvisoResponse> listar() {
        return avisoAdminService.listar().stream().map(AvisoResponse::from).toList();
    }

    @PutMapping("/{id}")
    public AvisoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody CriarAvisoRequest request) {
        return AvisoResponse.from(avisoAdminService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable UUID id) {
        avisoAdminService.excluir(id);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportarCsv() {
        String csv = avisoCsvService.exportar();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"avisos.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }

    @PostMapping("/import")
    public ResponseEntity<ImportResultResponse> importarCsv(@RequestParam("arquivo") MultipartFile arquivo) {
        ImportResultResponse response = avisoCsvService.importar(arquivo);
        if (!response.erros().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
