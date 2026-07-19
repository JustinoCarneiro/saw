package com.sawhub.hub.comercial;

import com.sawhub.hub.comercial.dto.ConciliacaoVendaResponse;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Modulo;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Change request 17/07/2026 ("conciliação") — separado de {@link ComercialController} de
 * propósito: é relatório financeiro (valor contratado x recebido, "importante pra declaração de
 * imposto"), não operação do funil comercial. {@code Modulo.FINANCEIRO} é o gate certo — mesmo
 * raciocínio já usado em {@code MentoradoContratoController} (M23) pra CNPJ/sócios/valor de
 * contrato. */
@RestController
@RequestMapping("/api/v1/admin/financeiro/conciliacao")
@RequiresModulo(Modulo.FINANCEIRO)
public class ConciliacaoController {

    private final ConciliacaoService conciliacaoService;

    public ConciliacaoController(ConciliacaoService conciliacaoService) {
        this.conciliacaoService = conciliacaoService;
    }

    @GetMapping
    public List<ConciliacaoVendaResponse> listar() {
        return conciliacaoService.listar();
    }
}
