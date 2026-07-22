package com.sawhub.hub.comercial;

import com.sawhub.hub.comercial.dto.CriarMetaComercialRequest;
import com.sawhub.hub.comercial.dto.MetaComercialResponse;
import com.sawhub.hub.team.Colaborador;
import com.sawhub.hub.team.ColaboradorRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Pedido do Marcos (22/07/2026, achado na auditoria de clareza — "metas e ranking, quem define e
 * onde?") — até esta leva, {@link MetaComercial} só existia via {@code DemoDataSeeder} (um único
 * registro fixo, Paula/julho-2026); não havia service, controller nem tela pra criar ou editar
 * meta nenhuma. Como {@link RankingComercialService#ranking} só lista vendedores que JÁ têm uma
 * linha de meta pro período, qualquer vendedor/mês sem seed manual no banco ficava
 * invisível no Ranking — não é só falta de UI, é a funcionalidade H13.3 incompleta. */
@Service
public class MetaComercialService {

    private final MetaComercialRepository metaComercialRepository;
    private final ColaboradorRepository colaboradorRepository;

    public MetaComercialService(MetaComercialRepository metaComercialRepository,
                                 ColaboradorRepository colaboradorRepository) {
        this.metaComercialRepository = metaComercialRepository;
        this.colaboradorRepository = colaboradorRepository;
    }

    /** Upsert por (vendedor, ano, mês) — definir de novo a meta do mesmo período corrige o
     * número, não duplica linha (mesmo critério já usado em CaixaMensalService.registrarPosicao). */
    @Transactional
    public MetaComercial definir(CriarMetaComercialRequest request) {
        Colaborador vendedor = colaboradorRepository.findById(request.vendedorId())
                .orElseThrow(() -> new IllegalArgumentException("Vendedor não encontrado."));

        MetaComercial meta = metaComercialRepository
                .findByVendedorIdAndAnoAndMes(request.vendedorId(), request.ano(), request.mes())
                .orElse(null);
        if (meta == null) {
            return metaComercialRepository.save(
                    new MetaComercial(vendedor, request.ano(), request.mes(), request.metaFechamentos(), request.percentualComissao()));
        }
        meta.atualizar(request.metaFechamentos(), request.percentualComissao());
        return metaComercialRepository.save(meta);
    }

    public List<MetaComercialResponse> listar(int ano, int mes) {
        return metaComercialRepository.buscarComVendedorPorPeriodo(ano, mes).stream()
                .map(MetaComercialResponse::from)
                .toList();
    }
}
