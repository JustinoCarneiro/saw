package com.sawhub.hub.loja;

import com.sawhub.hub.loja.dto.PedidoAdminResponse;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Modulo;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** H8.4 — visão admin de pedidos + ações manuais (reembolsar/cancelar). Ver Suposições do
 * Blueprint M14 no ROADMAP.md. */
@RestController
@RequestMapping("/api/v1/admin/pedidos")
@RequiresModulo(Modulo.COMERCIAL)
public class PedidoController {

    private final PedidoAdminService pedidoAdminService;

    public PedidoController(PedidoAdminService pedidoAdminService) {
        this.pedidoAdminService = pedidoAdminService;
    }

    @GetMapping
    public List<PedidoAdminResponse> listar(@RequestParam(required = false) StatusPedido status) {
        return pedidoAdminService.listar(status).stream().map(PedidoAdminResponse::from).toList();
    }

    @PatchMapping("/{id}/reembolsar")
    public PedidoAdminResponse reembolsar(@PathVariable UUID id) {
        return PedidoAdminResponse.from(pedidoAdminService.reembolsar(id));
    }

    @PatchMapping("/{id}/cancelar")
    public PedidoAdminResponse cancelar(@PathVariable UUID id) {
        return PedidoAdminResponse.from(pedidoAdminService.cancelar(id));
    }
}
