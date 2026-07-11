package com.sawhub.hub.financeiro;

import com.sawhub.hub.financeiro.dto.CriarLancamentoRequest;
import com.sawhub.hub.financeiro.dto.ImportResultResponse;
import com.sawhub.hub.financeiro.dto.LancamentoResponse;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Modulo;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/financeiro/lancamentos")
@RequiresModulo(Modulo.FINANCEIRO)
public class LancamentoController {

    private final LancamentoService lancamentoService;
    private final LancamentoCsvService lancamentoCsvService;

    public LancamentoController(LancamentoService lancamentoService, LancamentoCsvService lancamentoCsvService) {
        this.lancamentoService = lancamentoService;
        this.lancamentoCsvService = lancamentoCsvService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LancamentoResponse criar(@Valid @RequestBody CriarLancamentoRequest request) {
        return LancamentoResponse.from(lancamentoService.criar(request));
    }

    @GetMapping
    public List<LancamentoResponse> listar(@RequestParam LocalDate de, @RequestParam LocalDate ate,
                                            @RequestParam(required = false) TipoLancamento tipo,
                                            @RequestParam(required = false) UUID categoriaId) {
        return lancamentoService.listar(de, ate, tipo, categoriaId).stream().map(LancamentoResponse::from).toList();
    }

    // M21 — mesmos filtros de GET /lancamentos.
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportar(@RequestParam LocalDate de, @RequestParam LocalDate ate,
                                            @RequestParam(required = false) TipoLancamento tipo,
                                            @RequestParam(required = false) UUID categoriaId) {
        String csv = lancamentoCsvService.exportar(de, ate, tipo, categoriaId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"lancamentos.csv\"")
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
