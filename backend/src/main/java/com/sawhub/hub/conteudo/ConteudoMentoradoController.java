package com.sawhub.hub.conteudo;

import com.sawhub.hub.conteudo.dto.AtualizarConteudoMentoradoRequest;
import com.sawhub.hub.conteudo.dto.ConteudoMentoradoResponse;
import com.sawhub.hub.conteudo.dto.IndicadoresConsumoResponse;
import com.sawhub.hub.security.AppUserPrincipal;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mentorado/conteudos")
public class ConteudoMentoradoController {

    private final ConteudoMentoradoService conteudoMentoradoService;

    public ConteudoMentoradoController(ConteudoMentoradoService conteudoMentoradoService) {
        this.conteudoMentoradoService = conteudoMentoradoService;
    }

    @GetMapping
    public List<ConteudoMentoradoResponse> buscarCatalogo(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(required = false) TipoConteudo tipo,
            @RequestParam(required = false) Boolean favorito) {
        return conteudoMentoradoService.buscarCatalogo(principal.getUsuarioId(), tipo, favorito);
    }

    @GetMapping("/indicadores")
    public IndicadoresConsumoResponse indicadores(@AuthenticationPrincipal AppUserPrincipal principal) {
        return conteudoMentoradoService.indicadoresConsumo(principal.getUsuarioId());
    }

    @GetMapping("/dicas")
    public List<ConteudoMentoradoResponse> buscarDicas(
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return conteudoMentoradoService.buscarDicas(principal.getUsuarioId());
    }

    @PatchMapping("/{id}/favorito")
    public ConteudoMentoradoResponse favoritar(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody AtualizarConteudoMentoradoRequest request) {
        return conteudoMentoradoService.atualizarStatus(principal.getUsuarioId(), id,
                new AtualizarConteudoMentoradoRequest(request.favorito(), null));
    }

    @PatchMapping("/{id}/assistido")
    public ConteudoMentoradoResponse assistir(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody AtualizarConteudoMentoradoRequest request) {
        return conteudoMentoradoService.atualizarStatus(principal.getUsuarioId(), id,
                new AtualizarConteudoMentoradoRequest(null, request.assistido()));
    }
}
