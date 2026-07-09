package com.sawhub.hub.mentorado;

import com.sawhub.hub.mentorado.dto.AtualizarStatusTarefaRequest;
import com.sawhub.hub.mentorado.dto.AtualizarTarefaRequest;
import com.sawhub.hub.mentorado.dto.CriarTarefaRequest;
import com.sawhub.hub.mentorado.dto.ResumoTarefasResponse;
import com.sawhub.hub.mentorado.dto.TarefaResponse;
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

/** H4.1–H4.4 (M10) — `hasRole("MENTORADO")` já garantido pelo SecurityConfig
 * (`/api/v1/mentorado/**`); mesmo padrão de isolamento do M08/M09. */
@RestController
@RequestMapping("/api/v1/mentorado/tarefas")
public class TarefaController {

    private final TarefaService tarefaService;

    public TarefaController(TarefaService tarefaService) {
        this.tarefaService = tarefaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TarefaResponse criar(@AuthenticationPrincipal AppUserPrincipal principal, @Valid @RequestBody CriarTarefaRequest request) {
        return TarefaResponse.from(tarefaService.criar(principal.getUsuarioId(), request), LocalDate.now());
    }

    @GetMapping
    public List<TarefaResponse> listar(@AuthenticationPrincipal AppUserPrincipal principal,
                                        @RequestParam(required = false) StatusTarefa status,
                                        @RequestParam(required = false) String busca) {
        LocalDate hoje = LocalDate.now();
        return tarefaService.listar(principal.getUsuarioId(), status, busca).stream()
                .map(e -> TarefaResponse.from(e, hoje))
                .toList();
    }

    @GetMapping("/resumo")
    public ResumoTarefasResponse resumo(@AuthenticationPrincipal AppUserPrincipal principal) {
        return tarefaService.resumo(principal.getUsuarioId());
    }

    @PutMapping("/{id}")
    public TarefaResponse atualizar(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id,
                                     @Valid @RequestBody AtualizarTarefaRequest request) {
        return TarefaResponse.from(tarefaService.atualizar(principal.getUsuarioId(), id, request), LocalDate.now());
    }

    @PatchMapping("/{id}/status")
    public TarefaResponse atualizarStatus(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id,
                                           @Valid @RequestBody AtualizarStatusTarefaRequest request) {
        return TarefaResponse.from(tarefaService.avancarStatus(principal.getUsuarioId(), id, request.novoStatus()), LocalDate.now());
    }
}
