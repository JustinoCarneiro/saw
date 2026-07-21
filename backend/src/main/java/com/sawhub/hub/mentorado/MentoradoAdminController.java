package com.sawhub.hub.mentorado;

import com.sawhub.hub.mentorado.dto.AtualizarAcompanhamentoRequest;
import com.sawhub.hub.mentorado.dto.AtualizarDiagnosticoInicialRequest;
import com.sawhub.hub.mentorado.dto.AtualizarFerramentasObrigatoriasRequest;
import com.sawhub.hub.mentorado.dto.AtualizarMentoradoRequest;
import com.sawhub.hub.mentorado.dto.DiagnosticoInicialResponse;
import com.sawhub.hub.mentorado.dto.MentoradoCriadoResponse;
import com.sawhub.hub.mentorado.dto.MentoradoResponse;
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
    private final MentoradoCsvService mentoradoCsvService;

    public MentoradoAdminController(MentoradoAdminService mentoradoAdminService, MentoradoCsvService mentoradoCsvService) {
        this.mentoradoAdminService = mentoradoAdminService;
        this.mentoradoCsvService = mentoradoCsvService;
    }

    @GetMapping
    public List<MentoradoResponse> listar(@RequestParam(required = false) StatusMentorado status,
                                           @RequestParam(required = false) String busca) {
        return mentoradoAdminService.listar(status, busca).stream().map(MentoradoResponse::from).toList();
    }

    // M28 — "página dedicada de mentorado" (MentoradoDetalhePage), acessada direto por URL.
    @GetMapping("/{id}")
    public MentoradoResponse buscar(@PathVariable UUID id) {
        return MentoradoResponse.from(mentoradoAdminService.buscarPorId(id));
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

    // M23 — "/direto" e "/dados-contrato" moveram pra MentoradoContratoController
    // (Modulo.COMERCIAL, achado MÉDIO do revisor-seguranca: CNPJ/sócios/valor de contrato +
    // criação de credencial não deveriam ficar sob Modulo.MENTORADOS, que a área Gestão de
    // Performance também acessa). Diagnóstico Inicial fica aqui — é o trabalho da Leia
    // ("Sucesso do Gestor"/Gestão de Performance), não do Comercial.
    @GetMapping("/{id}/diagnostico-inicial")
    public DiagnosticoInicialResponse buscarDiagnosticoInicial(@PathVariable UUID id) {
        var diagnostico = mentoradoAdminService.buscarDiagnosticoInicial(id);
        return diagnostico != null ? DiagnosticoInicialResponse.from(diagnostico) : DiagnosticoInicialResponse.vazio();
    }

    @PatchMapping("/{id}/diagnostico-inicial")
    public DiagnosticoInicialResponse atualizarDiagnosticoInicial(
            @PathVariable UUID id, @Valid @RequestBody AtualizarDiagnosticoInicialRequest request) {
        return DiagnosticoInicialResponse.from(mentoradoAdminService.atualizarDiagnosticoInicial(id, request));
    }

    /** E17/M27 — as 4 ferramentas obrigatórias nomeadas do ranking (ver ROADMAP.md § "Blueprint
     * (M27)"). */
    @PatchMapping("/{id}/ferramentas-obrigatorias")
    public MentoradoResponse atualizarFerramentasObrigatorias(
            @PathVariable UUID id, @Valid @RequestBody AtualizarFerramentasObrigatoriasRequest request) {
        return MentoradoResponse.from(mentoradoAdminService.atualizarFerramentasObrigatorias(id, request));
    }

    /** E17/M27 — "dois eixos de acompanhamento" (engajamento + risco de churn), preenchimento
     * manual (ver ROADMAP.md § "Blueprint (M27)"). */
    @PatchMapping("/{id}/acompanhamento")
    public MentoradoResponse atualizarAcompanhamento(
            @PathVariable UUID id, @Valid @RequestBody AtualizarAcompanhamentoRequest request) {
        return MentoradoResponse.from(mentoradoAdminService.atualizarAcompanhamento(id, request));
    }

    // M22 — mesmos filtros de GET /mentorados.
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportar(@RequestParam(required = false) StatusMentorado status,
                                            @RequestParam(required = false) String busca) {
        String csv = mentoradoCsvService.exportar(status, busca);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mentorados.csv\"")
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
