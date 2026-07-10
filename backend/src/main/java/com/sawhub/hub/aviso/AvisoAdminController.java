package com.sawhub.hub.aviso;

import com.sawhub.hub.aviso.dto.AvisoResponse;
import com.sawhub.hub.aviso.dto.CriarAvisoRequest;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Modulo;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** H12.2 — gated por {@code Modulo.CONTEUDOS} (Suposição 5 do Blueprint M17: "publicar aviso" é,
 * na prática, a mesma ação de "publicar conteúdo" — mesma área RBAC, sem módulo novo). */
@RestController
@RequestMapping("/api/v1/admin/avisos")
@RequiresModulo(Modulo.CONTEUDOS)
public class AvisoAdminController {

    private final AvisoAdminService avisoAdminService;

    public AvisoAdminController(AvisoAdminService avisoAdminService) {
        this.avisoAdminService = avisoAdminService;
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
}
