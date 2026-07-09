package com.sawhub.hub.meta;

import com.sawhub.hub.meta.dto.AtualizarMetaRequest;
import com.sawhub.hub.meta.dto.AtualizarStatusMetaRequest;
import com.sawhub.hub.meta.dto.CriarMetaRequest;
import com.sawhub.hub.meta.dto.MetaResponse;
import com.sawhub.hub.meta.dto.ResumoMetasResponse;
import com.sawhub.hub.security.AppUserPrincipal;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** H3.1–H3.3 (M09) — `hasRole("MENTORADO")` já garantido pelo SecurityConfig
 * (`/api/v1/mentorado/**`); mesmo padrão de isolamento do M08 (id do mentorado só vem do
 * usuário autenticado). */
@RestController
@RequestMapping("/api/v1/mentorado/metas")
public class MetaController {

    private final MetaService metaService;

    public MetaController(MetaService metaService) {
        this.metaService = metaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MetaResponse criar(@AuthenticationPrincipal AppUserPrincipal principal, @Valid @RequestBody CriarMetaRequest request) {
        return MetaResponse.from(metaService.criar(principal.getUsuarioId(), request), LocalDate.now());
    }

    @GetMapping
    public List<MetaResponse> listar(@AuthenticationPrincipal AppUserPrincipal principal,
                                      @RequestParam(required = false) StatusMeta status) {
        LocalDate hoje = LocalDate.now();
        return metaService.listar(principal.getUsuarioId(), status).stream()
                .map(m -> MetaResponse.from(m, hoje))
                .toList();
    }

    @GetMapping("/resumo")
    public ResumoMetasResponse resumo(@AuthenticationPrincipal AppUserPrincipal principal) {
        return metaService.resumo(principal.getUsuarioId());
    }

    @PutMapping("/{id}")
    public MetaResponse atualizar(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id,
                                   @Valid @RequestBody AtualizarMetaRequest request) {
        return MetaResponse.from(metaService.atualizar(principal.getUsuarioId(), id, request), LocalDate.now());
    }

    @PatchMapping("/{id}/status")
    public MetaResponse atualizarStatus(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id,
                                         @Valid @RequestBody AtualizarStatusMetaRequest request) {
        return MetaResponse.from(metaService.avancarStatus(principal.getUsuarioId(), id, request.novoStatus()), LocalDate.now());
    }
}
