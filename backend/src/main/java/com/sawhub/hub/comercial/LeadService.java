package com.sawhub.hub.comercial;

import com.sawhub.hub.atividade.AtividadeLogService;
import com.sawhub.hub.comercial.dto.AvancarLeadRequest;
import com.sawhub.hub.comercial.dto.CriarLeadRequest;
import com.sawhub.hub.team.Colaborador;
import com.sawhub.hub.team.ColaboradorRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** H1.3 (solicitar acesso) + H13.2 (funil comercial). */
@Service
public class LeadService {

    private final LeadRepository leadRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final AtividadeLogService atividadeLogService;

    public LeadService(LeadRepository leadRepository, ColaboradorRepository colaboradorRepository,
                        AtividadeLogService atividadeLogService) {
        this.leadRepository = leadRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.atividadeLogService = atividadeLogService;
    }

    @Transactional
    public Lead criar(CriarLeadRequest request) {
        Lead lead = new Lead(request.nome(), request.email(), request.telefone(), request.mensagem(),
                request.planoInteresse());
        return leadRepository.save(lead);
    }

    public List<Lead> listar(StatusLead status, UUID vendedorId) {
        return leadRepository.buscarComFiltro(status, vendedorId);
    }

    @Transactional
    public Lead avancar(UUID leadId, AvancarLeadRequest request) {
        Lead lead = leadRepository.buscarPorIdComVendedor(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Lead não encontrado."));

        switch (request.novoStatus()) {
            case EM_CONTATO -> lead.moverParaEmContato(resolverVendedor(request.vendedorId()));
            case PROPOSTA -> lead.moverParaProposta();
            // Achado L2 da revisão de segurança: sem exigir plano/motivo aqui, um FECHADO sem
            // plano quebra silenciosamente "vendas por plano" (H13.1), e um PERDIDO sem motivo
            // perde o dado que justifica a decisão pro time comercial revisar depois.
            case FECHADO -> {
                if (request.planoFechado() == null) {
                    throw new IllegalArgumentException("Plano fechado é obrigatório para mover o lead para Fechado.");
                }
                lead.fechar(request.planoFechado());
                atividadeLogService.registrar("LEAD_FECHADO", "Lead fechado: " + lead.getNome());
            }
            case PERDIDO -> {
                if (request.motivoPerdido() == null || request.motivoPerdido().isBlank()) {
                    throw new IllegalArgumentException("Motivo é obrigatório para marcar o lead como Perdido.");
                }
                lead.perder(request.motivoPerdido());
                atividadeLogService.registrar("LEAD_PERDIDO", "Lead perdido: " + lead.getNome());
            }
            case SOLICITACAO -> throw new IllegalArgumentException("Não é possível mover um lead de volta para Solicitação.");
        }

        return leadRepository.save(lead);
    }

    private Colaborador resolverVendedor(UUID vendedorId) {
        if (vendedorId == null) {
            throw new IllegalArgumentException("Vendedor é obrigatório para mover o lead para Em contato.");
        }
        return colaboradorRepository.findById(vendedorId)
                .orElseThrow(() -> new IllegalArgumentException("Vendedor não encontrado."));
    }
}
