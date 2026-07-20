package com.sawhub.hub.financeiro;

import com.sawhub.hub.common.dto.ImportResultResponse;
import com.sawhub.hub.financeiro.dto.LancamentoResponse;
import com.sawhub.hub.financeiro.dto.LiquidarLancamentoRequest;
import com.sawhub.hub.financeiro.dto.LiquidarParcialLancamentoRequest;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Modulo;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PostMapping;

/** M26 — "conta a pagar/receber" deixou de ser uma entidade própria (ver ROADMAP.md §
 * "Blueprint (M26)"): este controller é um recorte fino, por `dataVencimento`, sobre a mesma
 * tabela/serviço de `LancamentoController` — mantido como path separado só por compatibilidade
 * de UX (a tela "Contas" continua existindo), não porque o dado seja diferente. */
@RestController
@RequestMapping("/api/v1/admin/financeiro/contas")
@RequiresModulo(Modulo.FINANCEIRO)
public class ContaController {

    private final LancamentoService lancamentoService;
    private final LancamentoCsvService lancamentoCsvService;

    public ContaController(LancamentoService lancamentoService, LancamentoCsvService lancamentoCsvService) {
        this.lancamentoService = lancamentoService;
        this.lancamentoCsvService = lancamentoCsvService;
    }

    @GetMapping
    public List<LancamentoResponse> listar(@RequestParam(required = false) TipoLancamento tipo,
                                            @RequestParam(required = false) StatusLancamento status,
                                            @RequestParam(required = false) Integer ano,
                                            @RequestParam(required = false) Integer mes,
                                            @RequestParam(required = false) UUID eventoId) {
        return lancamentoService.listarPorVencimento(tipo, status, ano, mes, eventoId).stream()
                .map(LancamentoResponse::from).toList();
    }

    @PatchMapping("/{id}/liquidar")
    public LancamentoResponse liquidar(@PathVariable UUID id, @Valid @RequestBody LiquidarLancamentoRequest request) {
        return LancamentoResponse.from(lancamentoService.liquidar(id, request));
    }

    @PatchMapping("/{id}/liquidar-parcial")
    public LancamentoResponse liquidarParcial(@PathVariable UUID id,
                                               @Valid @RequestBody LiquidarParcialLancamentoRequest request) {
        return LancamentoResponse.from(lancamentoService.liquidarParcial(id, request));
    }

    // M21 — mesmos filtros de GET /contas.
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportar(@RequestParam(required = false) TipoLancamento tipo,
                                            @RequestParam(required = false) StatusLancamento status,
                                            @RequestParam(required = false) Integer ano,
                                            @RequestParam(required = false) Integer mes,
                                            @RequestParam(required = false) UUID eventoId) {
        String csv = lancamentoCsvService.exportarPorVencimento(tipo, status, ano, mes, eventoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"contas.csv\"")
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    // M21 — 200 se tudo foi validado e persistido, 422 se nada foi (ver erros no corpo).
    @PostMapping("/import")
    public ResponseEntity<ImportResultResponse> importar(@RequestParam("arquivo") MultipartFile arquivo) {
        ImportResultResponse resultado = lancamentoCsvService.importar(arquivo);
        HttpStatus status = resultado.erros().isEmpty() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(resultado);
    }
}
