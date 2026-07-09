package com.sawhub.hub.mentorado;

import com.sawhub.hub.mentorado.dto.AtualizarMentoradoRequest;
import com.sawhub.hub.mentorado.dto.MentoradoCriadoResponse;
import com.sawhub.hub.mentorado.dto.MentoradoResponse;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Modulo;
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

/** H11.1 — CRUD administrativo de mentorados. */
@RestController
@RequestMapping("/api/v1/admin/mentorados")
@RequiresModulo(Modulo.MENTORADOS)
public class MentoradoAdminController {

    private final MentoradoAdminService mentoradoAdminService;

    public MentoradoAdminController(MentoradoAdminService mentoradoAdminService) {
        this.mentoradoAdminService = mentoradoAdminService;
    }

    @GetMapping
    public List<MentoradoResponse> listar(@RequestParam(required = false) Plano plano,
                                           @RequestParam(required = false) StatusMentorado status,
                                           @RequestParam(required = false) String busca) {
        return mentoradoAdminService.listar(plano, status, busca).stream().map(MentoradoResponse::from).toList();
    }

    @PutMapping("/{id}")
    public MentoradoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarMentoradoRequest request) {
        return MentoradoResponse.from(mentoradoAdminService.atualizar(id, request));
    }

    @PatchMapping("/{id}/ativar")
    public MentoradoResponse ativar(@PathVariable UUID id) {
        return MentoradoResponse.from(mentoradoAdminService.ativar(id));
    }

    @PatchMapping("/{id}/desativar")
    public MentoradoResponse desativar(@PathVariable UUID id) {
        return MentoradoResponse.from(mentoradoAdminService.desativar(id));
    }

    @PostMapping("/a-partir-do-lead/{leadId}")
    @ResponseStatus(HttpStatus.CREATED)
    public MentoradoCriadoResponse criarAPartirDeLead(@PathVariable UUID leadId) {
        var resultado = mentoradoAdminService.criarAPartirDeLead(leadId);
        return MentoradoCriadoResponse.from(resultado.mentorado(), resultado.senhaTemporaria());
    }
}
