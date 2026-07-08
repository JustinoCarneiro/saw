package com.sawhub.hub.financeiro;

import com.sawhub.hub.financeiro.dto.CriarLancamentoRequest;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** H14.1 — lançamento de receitas/despesas. */
@Service
public class LancamentoService {

    private final LancamentoFinanceiroRepository lancamentoRepository;
    private final CategoriaFinanceiraRepository categoriaRepository;

    public LancamentoService(LancamentoFinanceiroRepository lancamentoRepository,
                              CategoriaFinanceiraRepository categoriaRepository) {
        this.lancamentoRepository = lancamentoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional
    public LancamentoFinanceiro criar(CriarLancamentoRequest request) {
        CategoriaFinanceira categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada."));
        LancamentoFinanceiro lancamento = new LancamentoFinanceiro(request.tipo(), categoria, request.descricao(),
                request.valor(), request.dataCompetencia(), request.status(), request.planoReferencia());
        return lancamentoRepository.save(lancamento);
    }

    public List<LancamentoFinanceiro> listar(LocalDate de, LocalDate ate, TipoLancamento tipo, java.util.UUID categoriaId) {
        return lancamentoRepository.findByDataCompetenciaBetweenOrderByDataCompetenciaDesc(de, ate).stream()
                .filter(l -> tipo == null || l.getTipo() == tipo)
                .filter(l -> categoriaId == null || l.getCategoria().getId().equals(categoriaId))
                .toList();
    }
}
