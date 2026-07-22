package com.sawhub.hub.mentorado;

import com.sawhub.hub.common.dto.ImportResultResponse;
import com.sawhub.hub.mentorado.dto.AtualizarEncaminhamentoAdminRequest;
import com.sawhub.hub.mentorado.dto.AtualizarStatusTarefaRequest;
import com.sawhub.hub.mentorado.dto.EncaminhamentoAdminResponse;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** M23 — primeiros endpoints Admin de CRUD direto para Encaminhamento (o painel consolidado no M17
 * só via dados agregados; a criação/edição era 100% self-service do mentorado ou gerada via
 * publicação de ata). Fase 5 (H4.6): {@code listar()} fecha o gap achado numa revisão ao vivo — só
 * existiam export/import, sem nenhuma tela pra conferir o resultado dessas ações. {@code atualizar}/
 * {@code atualizarStatus} (22/07/2026, pedido do Marcos) fecham outro gap: a tela também não tinha
 * como editar título/prazo/prioridade nem avançar status de um encaminhamento existente — inclusive
 * os materializados na publicação de ata, que nunca têm prazo de origem. */
@RestController
@RequestMapping("/api/v1/admin/encaminhamentos")
@RequiresModulo(Modulo.MENTORADOS)
public class EncaminhamentoAdminController {

    private final EncaminhamentoCsvService encaminhamentoCsvService;
    private final EncaminhamentoRepository encaminhamentoRepository;
    private final EncaminhamentoAdminService encaminhamentoAdminService;

    public EncaminhamentoAdminController(EncaminhamentoCsvService encaminhamentoCsvService,
                                          EncaminhamentoRepository encaminhamentoRepository,
                                          EncaminhamentoAdminService encaminhamentoAdminService) {
        this.encaminhamentoCsvService = encaminhamentoCsvService;
        this.encaminhamentoRepository = encaminhamentoRepository;
        this.encaminhamentoAdminService = encaminhamentoAdminService;
    }

    @GetMapping
    public List<EncaminhamentoAdminResponse> listar() {
        return encaminhamentoRepository.listarTodasComMentorado().stream()
                .map(EncaminhamentoAdminResponse::from).toList();
    }

    @PutMapping("/{id}")
    public EncaminhamentoAdminResponse atualizar(@PathVariable UUID id,
                                                  @Valid @RequestBody AtualizarEncaminhamentoAdminRequest request) {
        return EncaminhamentoAdminResponse.from(encaminhamentoAdminService.atualizar(id, request));
    }

    @PatchMapping("/{id}/status")
    public EncaminhamentoAdminResponse atualizarStatus(@PathVariable UUID id,
                                                         @Valid @RequestBody AtualizarStatusTarefaRequest request) {
        return EncaminhamentoAdminResponse.from(encaminhamentoAdminService.avancarStatus(id, request.novoStatus()));
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
