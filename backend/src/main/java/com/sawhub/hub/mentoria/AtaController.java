package com.sawhub.hub.mentoria;

import com.sawhub.hub.mentoria.dto.AtaResponse;
import com.sawhub.hub.mentoria.dto.AtualizarDecisoesRequest;
import com.sawhub.hub.mentoria.dto.AtualizarResumoRequest;
import com.sawhub.hub.mentoria.dto.AtualizarSugestaoRequest;
import com.sawhub.hub.mentoria.dto.ColarTranscricaoRequest;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Modulo;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** H5.2 + diferencial de IA (ROADMAP.md M06). */
@RestController
@RequestMapping("/api/v1/admin/mentorias/{mentoriaId}/ata")
@RequiresModulo(Modulo.MENTORADOS)
public class AtaController {

    private final AtaService ataService;

    public AtaController(AtaService ataService) {
        this.ataService = ataService;
    }

    @GetMapping
    public AtaResponse buscar(@PathVariable UUID mentoriaId) {
        Ata ata = ataService.buscarPorMentoria(mentoriaId);
        return AtaResponse.from(ata, ataService.listarSugestoes(mentoriaId));
    }

    @PostMapping("/audio")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AtaResponse enviarAudio(@PathVariable UUID mentoriaId, @RequestParam("arquivo") MultipartFile arquivo) {
        Ata ata = ataService.iniciarUpload(mentoriaId, arquivo);
        return AtaResponse.from(ata, ataService.listarSugestoes(mentoriaId));
    }

    // M28 (change request, 21/07/2026) — "colar transcrição do Google Meet", alternativa ao
    // upload de áudio acima (aditivo, o mentor escolhe qual usar).
    @PostMapping("/transcricao")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AtaResponse colarTranscricao(@PathVariable UUID mentoriaId, @Valid @RequestBody ColarTranscricaoRequest request) {
        Ata ata = ataService.iniciarComTranscricaoColada(mentoriaId, request.transcricao());
        return AtaResponse.from(ata, ataService.listarSugestoes(mentoriaId));
    }

    @PatchMapping
    public AtaResponse editarResumo(@PathVariable UUID mentoriaId, @Valid @RequestBody AtualizarResumoRequest request) {
        Ata ata = ataService.editarResumo(mentoriaId, request.resumo());
        return AtaResponse.from(ata, ataService.listarSugestoes(mentoriaId));
    }

    // Change request 17/07/2026 ("campo Decisões na ata") — sub-path próprio pra não colidir com
    // o PATCH sem sufixo (editarResumo).
    @PatchMapping("/decisoes")
    public AtaResponse editarDecisoes(@PathVariable UUID mentoriaId, @Valid @RequestBody AtualizarDecisoesRequest request) {
        Ata ata = ataService.editarDecisoes(mentoriaId, request.decisoes());
        return AtaResponse.from(ata, ataService.listarSugestoes(mentoriaId));
    }

    @PatchMapping("/sugestoes/{sugestaoId}")
    public AtaResponse editarSugestao(@PathVariable UUID mentoriaId, @PathVariable UUID sugestaoId,
                                       @Valid @RequestBody AtualizarSugestaoRequest request) {
        ataService.editarSugestao(mentoriaId, sugestaoId, request.titulo(), request.pesoSugerido(), request.aceito());
        Ata ata = ataService.buscarPorMentoria(mentoriaId);
        return AtaResponse.from(ata, ataService.listarSugestoes(mentoriaId));
    }

    @PostMapping("/publicar")
    public AtaResponse publicar(@PathVariable UUID mentoriaId) {
        Ata ata = ataService.publicar(mentoriaId);
        return AtaResponse.from(ata, ataService.listarSugestoes(mentoriaId));
    }
}
