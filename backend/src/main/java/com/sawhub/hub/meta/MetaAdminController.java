package com.sawhub.hub.meta;

import com.sawhub.hub.common.dto.ImportResultResponse;
import com.sawhub.hub.meta.dto.AtualizarMetaRequest;
import com.sawhub.hub.meta.dto.AtualizarStatusMetaRequest;
import com.sawhub.hub.meta.dto.CriarMetaAdminRequest;
import com.sawhub.hub.meta.dto.MetaAdminResponse;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Modulo;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** M23 — primeiros endpoints Admin para o módulo de Metas (que até então era 100% self-service
 * do mentorado, M09). O import em massa pelo Admin apoia o onboarding de novos mentorados.
 * Fase 5 (H3.4): {@code listar()} fecha o gap achado numa revisão ao vivo — só existiam
 * export/import, sem nenhuma tela pra conferir o resultado dessas ações. {@code criar}/
 * {@code atualizar}/{@code atualizarStatus} (22/07/2026, pedido do Marcos) fecham outro gap:
 * a tela também não tinha como criar ou editar uma meta direto, nem avançar status. */
@RestController
@RequestMapping("/api/v1/admin/metas")
@RequiresModulo(Modulo.MENTORADOS)
public class MetaAdminController {

    private final MetaCsvService metaCsvService;
    private final MetaRepository metaRepository;
    private final MetaAdminService metaAdminService;

    public MetaAdminController(MetaCsvService metaCsvService, MetaRepository metaRepository,
                                MetaAdminService metaAdminService) {
        this.metaCsvService = metaCsvService;
        this.metaRepository = metaRepository;
        this.metaAdminService = metaAdminService;
    }

    @GetMapping
    public List<MetaAdminResponse> listar() {
        return metaRepository.listarTodasComMentorado().stream().map(MetaAdminResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MetaAdminResponse criar(@Valid @RequestBody CriarMetaAdminRequest request) {
        return MetaAdminResponse.from(metaAdminService.criar(request));
    }

    @PutMapping("/{id}")
    public MetaAdminResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarMetaRequest request) {
        return MetaAdminResponse.from(metaAdminService.atualizar(id, request));
    }

    @PatchMapping("/{id}/status")
    public MetaAdminResponse atualizarStatus(@PathVariable UUID id, @Valid @RequestBody AtualizarStatusMetaRequest request) {
        return MetaAdminResponse.from(metaAdminService.avancarStatus(id, request.novoStatus()));
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
