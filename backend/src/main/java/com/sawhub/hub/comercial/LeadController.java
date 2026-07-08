package com.sawhub.hub.comercial;

import com.sawhub.hub.comercial.dto.CriarLeadRequest;
import com.sawhub.hub.comercial.dto.LeadCriadoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** H1.3 — "Solicitar acesso". Único endpoint sem autenticação além do login (ver
 * SecurityConfig.permitAll e a nota de risco no ROADMAP.md M05): resposta minimalista de
 * propósito, não expõe dado de pipeline comercial pra quem não está logado. */
@RestController
@RequestMapping("/api/v1/leads")
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeadCriadoResponse solicitarAcesso(@Valid @RequestBody CriarLeadRequest request) {
        return LeadCriadoResponse.from(leadService.criar(request));
    }
}
