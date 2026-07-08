package com.sawhub.hub.financeiro;

import com.sawhub.hub.financeiro.dto.ContaResponse;
import com.sawhub.hub.financeiro.dto.CriarContaRequest;
import com.sawhub.hub.financeiro.dto.LiquidarContaRequest;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/financeiro/contas")
@RequiresModulo(Modulo.FINANCEIRO)
public class ContaController {

    private final ContaPagarReceberService contaService;

    public ContaController(ContaPagarReceberService contaService) {
        this.contaService = contaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContaResponse criar(@Valid @RequestBody CriarContaRequest request) {
        return ContaResponse.from(contaService.criar(request));
    }

    @GetMapping
    public List<ContaResponse> listar(@RequestParam(required = false) TipoConta tipo,
                                       @RequestParam(required = false) StatusConta status) {
        return contaService.listar(tipo, status).stream().map(ContaResponse::from).toList();
    }

    @PatchMapping("/{id}/liquidar")
    public ContaResponse liquidar(@PathVariable UUID id, @Valid @RequestBody LiquidarContaRequest request) {
        return ContaResponse.from(contaService.liquidar(id, request));
    }
}
