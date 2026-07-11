package com.sawhub.hub.financeiro;

import com.sawhub.hub.financeiro.dto.ContaResponse;
import com.sawhub.hub.financeiro.dto.CriarContaRequest;
import com.sawhub.hub.financeiro.dto.ImportResultResponse;
import com.sawhub.hub.financeiro.dto.LiquidarContaRequest;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/financeiro/contas")
@RequiresModulo(Modulo.FINANCEIRO)
public class ContaController {

    private final ContaPagarReceberService contaService;
    private final ContaCsvService contaCsvService;

    public ContaController(ContaPagarReceberService contaService, ContaCsvService contaCsvService) {
        this.contaService = contaService;
        this.contaCsvService = contaCsvService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContaResponse criar(@Valid @RequestBody CriarContaRequest request) {
        return ContaResponse.from(contaService.criar(request));
    }

    @GetMapping
    public List<ContaResponse> listar(@RequestParam(required = false) TipoConta tipo,
                                       @RequestParam(required = false) StatusConta status) {
        return contaService.listar(tipo, status).stream().map(ContaResponse::from).toList();
    }

    @PatchMapping("/{id}/liquidar")
    public ContaResponse liquidar(@PathVariable UUID id, @Valid @RequestBody LiquidarContaRequest request) {
        return ContaResponse.from(contaService.liquidar(id, request));
    }

    // M21 — mesmos filtros de GET /contas.
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportar(@RequestParam(required = false) TipoConta tipo,
                                            @RequestParam(required = false) StatusConta status) {
        String csv = contaCsvService.exportar(tipo, status);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"contas.csv\"")
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    // M21 — 200 se tudo foi validado e persistido, 422 se nada foi (ver erros no corpo).
    @PostMapping("/import")
    public ResponseEntity<ImportResultResponse> importar(@RequestParam("arquivo") MultipartFile arquivo) {
        ImportResultResponse resultado = contaCsvService.importar(arquivo);
        HttpStatus status = resultado.erros().isEmpty() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(resultado);
    }
}
