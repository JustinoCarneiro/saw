package com.sawhub.hub.mentoria;

import com.sawhub.hub.mentoria.dto.MentoriaMentoradoResponse;
import com.sawhub.hub.security.AppUserPrincipal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** H5.1-H5.3 (M12) — `hasRole("MENTORADO")` já garantido pelo SecurityConfig
 * (`/api/v1/mentorado/**`); mesmo padrão de isolamento do M08-M11: o id do Mentorado nunca vem de
 * parâmetro de request, só do usuário autenticado. */
@RestController
@RequestMapping("/api/v1/mentorado/mentorias")
public class MentoriaMentoradoController {

    private final MentoriaMentoradoService mentoriaMentoradoService;

    public MentoriaMentoradoController(MentoriaMentoradoService mentoriaMentoradoService) {
        this.mentoriaMentoradoService = mentoriaMentoradoService;
    }

    @GetMapping
    public List<MentoriaMentoradoResponse> listar(@AuthenticationPrincipal AppUserPrincipal principal) {
        return mentoriaMentoradoService.listar(principal.getUsuarioId());
    }

    @GetMapping("/{id}/calendario.ics")
    public ResponseEntity<byte[]> calendario(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        byte[] ics = mentoriaMentoradoService.gerarIcs(principal.getUsuarioId(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mentoria.ics\"")
                .body(ics);
    }
}
