package com.sawhub.hub.loja;

import com.sawhub.hub.atividade.AtividadeLogService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** H8.4 — visão e ações manuais do Admin sobre pedidos. Reembolsar/cancelar aqui só refletem o
 * estado no SAW HUB — o estorno de verdade acontece direto no painel do Mercado Pago (ver
 * Suposições do Blueprint M14 no ROADMAP.md, mesma categoria de pendência do M12/M13). */
@Service
public class PedidoAdminService {

    private final PedidoRepository pedidoRepository;
    private final AtividadeLogService atividadeLogService;

    public PedidoAdminService(PedidoRepository pedidoRepository, AtividadeLogService atividadeLogService) {
        this.pedidoRepository = pedidoRepository;
        this.atividadeLogService = atividadeLogService;
    }

    public List<Pedido> listar(StatusPedido status) {
        return pedidoRepository.buscarParaAdmin(status);
    }

    @Transactional
    public Pedido reembolsar(UUID id) {
        Pedido pedido = buscar(id);
        pedido.reembolsar();
        atividadeLogService.registrar("PEDIDO_REEMBOLSADO", "Pedido reembolsado: " + pedido.getMentorado().getNome());
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido cancelar(UUID id) {
        Pedido pedido = buscar(id);
        pedido.cancelar();
        return pedidoRepository.save(pedido);
    }

    private Pedido buscar(UUID id) {
        return pedidoRepository.buscarPorIdComItens(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado."));
    }
}
