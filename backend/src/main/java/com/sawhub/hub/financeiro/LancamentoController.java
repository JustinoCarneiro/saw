package com.sawhub.hub.financeiro;

import com.sawhub.hub.financeiro.dto.CriarLancamentoRequest;
import com.sawhub.hub.financeiro.dto.LancamentoResponse;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Modulo;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/financeiro/lancamentos")
@RequiresModulo(Modulo.FINANCEIRO)
public class LancamentoController {

    private final LancamentoService lancamentoService;

    public LancamentoController(LancamentoService lancamentoService) {
        this.lancamentoService = lancamentoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LancamentoResponse criar(@Valid @RequestBody CriarLancamentoRequest request) {
        return LancamentoResponse.from(lancamentoService.criar(request));
    }

    @GetMapping
    public List<LancamentoResponse> listar(@RequestParam LocalDate de, @RequestParam LocalDate ate,
                                            @RequestParam(required = false) TipoLancamento tipo,
                                            @RequestParam(required = false) UUID categoriaId) {
        return lancamentoService.listar(de, ate, tipo, categoriaId).stream().map(LancamentoResponse::from).toList();
    }
}
