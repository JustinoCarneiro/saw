package com.sawhub.hub.mentorado;

import com.sawhub.hub.mentorado.dto.AtualizarDadosContratoRequest;
import com.sawhub.hub.mentorado.dto.CriarMentoradoDiretoRequest;
import com.sawhub.hub.mentorado.dto.MentoradoCriadoResponse;
import com.sawhub.hub.mentorado.dto.MentoradoResponse;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Modulo;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

/** M23 (change request pós-MVP, 17/07/2026) — separado de {@link MentoradoAdminController} de
 * propósito: achado MÉDIO do revisor-seguranca dessa leva. CNPJ, sócios e valor de contrato são
 * dado comercial/financeiro sensível, e "criar mentorado direto" gera credencial de login — nenhum
 * dos dois deveria ficar sob {@code Modulo.MENTORADOS} (a área Gestão de Performance também tem
 * esse módulo, per CLAUDE.md ela só deveria ver Mentorados/Mentorias/Conteúdos/Painel
 * Consolidado, não dado comercial/financeiro). {@code Modulo.COMERCIAL} é o gate certo — Admin
 * continua com acesso total (EnumSet.allOf em AreaModuloMatrix). */
@RestController
@RequestMapping("/api/v1/admin/mentorados")
@RequiresModulo(Modulo.COMERCIAL)
public class MentoradoContratoController {

    private final MentoradoAdminService mentoradoAdminService;

    public MentoradoContratoController(MentoradoAdminService mentoradoAdminService) {
        this.mentoradoAdminService = mentoradoAdminService;
    }

    // "criar mentorado direto" (pedido explícito do cliente), sem exigir um Lead pré-existente no funil.
    @PostMapping("/direto")
    @ResponseStatus(HttpStatus.CREATED)
    public MentoradoCriadoResponse criarDireto(@Valid @RequestBody CriarMentoradoDiretoRequest request) {
        var resultado = mentoradoAdminService.criarDireto(request);
        return MentoradoCriadoResponse.from(resultado.mentorado(), resultado.senhaTemporaria());
    }

    @PatchMapping("/{id}/dados-contrato")
    public MentoradoResponse atualizarDadosContrato(@PathVariable UUID id,
                                                      @Valid @RequestBody AtualizarDadosContratoRequest request) {
        return MentoradoResponse.from(mentoradoAdminService.atualizarDadosContrato(id, request));
    }

    @PostMapping("/{id}/documento-contrato")
    public MentoradoResponse enviarDocumentoContrato(@PathVariable UUID id, @RequestParam("arquivo") MultipartFile arquivo) {
        return MentoradoResponse.from(mentoradoAdminService.salvarDocumentoContrato(id, arquivo));
    }

    @GetMapping("/{id}/documento-contrato")
    public ResponseEntity<byte[]> baixarDocumentoContrato(@PathVariable UUID id) {
        Path arquivo = mentoradoAdminService.resolverDocumentoContrato(id);
        byte[] conteudo;
        try {
            conteudo = Files.readAllBytes(arquivo);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler o documento do contrato.", e);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"contrato.pdf\"")
                .body(conteudo);
    }
}
