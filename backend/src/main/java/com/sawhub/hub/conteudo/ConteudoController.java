package com.sawhub.hub.conteudo;

import com.sawhub.hub.conteudo.dto.AtualizarConteudoRequest;
import com.sawhub.hub.conteudo.dto.ConteudoResponse;
import com.sawhub.hub.conteudo.dto.CriarConteudoRequest;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Modulo;
import com.sawhub.hub.common.dto.ImportResultResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** H11.3 — CRUD da biblioteca de conteúdos. */
@RestController
@RequestMapping("/api/v1/admin/conteudos")
@RequiresModulo(Modulo.CONTEUDOS)
public class ConteudoController {

    private final ConteudoService conteudoService;
    private final ConteudoCsvService conteudoCsvService;

    public ConteudoController(ConteudoService conteudoService, ConteudoCsvService conteudoCsvService) {
        this.conteudoService = conteudoService;
        this.conteudoCsvService = conteudoCsvService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConteudoResponse criar(@Valid @RequestBody CriarConteudoRequest request) {
        return ConteudoResponse.from(conteudoService.criar(request));
    }

    @GetMapping
    public List<ConteudoResponse> listar(@RequestParam(required = false) TipoConteudo tipo,
                                          @RequestParam(required = false) Boolean publicado) {
        return conteudoService.listar(tipo, publicado).stream().map(ConteudoResponse::from).toList();
    }

    @PutMapping("/{id}")
    public ConteudoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarConteudoRequest request) {
        return ConteudoResponse.from(conteudoService.atualizar(id, request));
    }

    @PatchMapping("/{id}/publicar")
    public ConteudoResponse publicar(@PathVariable UUID id) {
        return ConteudoResponse.from(conteudoService.publicar(id));
    }

    @PatchMapping("/{id}/despublicar")
    public ConteudoResponse despublicar(@PathVariable UUID id) {
        return ConteudoResponse.from(conteudoService.despublicar(id));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportarCsv(@RequestParam(required = false) TipoConteudo tipo,
                                               @RequestParam(required = false) Boolean publicado) {
        String csv = conteudoCsvService.exportar(tipo, publicado);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"conteudos.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }

    @PostMapping("/import")
    public ResponseEntity<ImportResultResponse> importarCsv(@RequestParam("arquivo") MultipartFile arquivo) {
        ImportResultResponse response = conteudoCsvService.importar(arquivo);
        if (!response.erros().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
