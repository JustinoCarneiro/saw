package com.sawhub.hub.financeiro;

import com.sawhub.hub.evento.Evento;
import com.sawhub.hub.evento.EventoRepository;
import com.sawhub.hub.financeiro.dto.CriarLancamentoRequest;
import com.sawhub.hub.financeiro.dto.LiquidarLancamentoRequest;
import com.sawhub.hub.financeiro.dto.LiquidarParcialLancamentoRequest;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** H14.1 + H14.4 — lançamento de receitas/despesas, incluindo o que antes vivia em
 * `ContaPagarReceberService` (M26, merge de entidade — ver ROADMAP.md § "Blueprint (M26)"). */
@Service
public class LancamentoService {

    // Sentinela pro filtro de período "desligado" — Postgres não infere tipo de um parâmetro
    // LocalDate nulo numa query com tantas colunas bytea de pgcrypto ao redor (ver comentário em
    // LancamentoFinanceiroRepository.buscarComFiltroPorVencimento).
    private static final LocalDate SEM_FILTRO_INICIO = LocalDate.of(1900, 1, 1);
    private static final LocalDate SEM_FILTRO_FIM = LocalDate.of(2999, 12, 31);

    private final LancamentoFinanceiroRepository lancamentoRepository;
    private final CategoriaFinanceiraRepository categoriaRepository;
    private final EventoRepository eventoRepository;

    public LancamentoService(LancamentoFinanceiroRepository lancamentoRepository,
                              CategoriaFinanceiraRepository categoriaRepository,
                              EventoRepository eventoRepository) {
        this.lancamentoRepository = lancamentoRepository;
        this.categoriaRepository = categoriaRepository;
        this.eventoRepository = eventoRepository;
    }

    @Transactional
    public LancamentoFinanceiro criar(CriarLancamentoRequest request) {
        CategoriaFinanceira categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada."));
        Evento evento = request.eventoId() == null ? null
                : eventoRepository.findById(request.eventoId())
                        .orElseThrow(() -> new IllegalArgumentException("Evento não encontrado."));
        LancamentoFinanceiro lancamento = new LancamentoFinanceiro(request.tipo(), categoria, request.descricao(),
                request.valor(), request.dataCompetencia(), request.status(), evento,
                request.dataVencimento());
        return lancamentoRepository.save(lancamento);
    }

    public List<LancamentoFinanceiro> listar(LocalDate de, LocalDate ate, TipoLancamento tipo, UUID categoriaId) {
        return listar(de, ate, tipo, categoriaId, null, null);
    }

    /** `GET /admin/financeiro/lancamentos` — filtra por `dataCompetencia` (obrigatória, mesmo
     * comportamento de sempre); `status`/`eventoId` nulos desligam o respectivo filtro. */
    public List<LancamentoFinanceiro> listar(LocalDate de, LocalDate ate, TipoLancamento tipo, UUID categoriaId,
                                              StatusLancamento status, UUID eventoId) {
        return lancamentoRepository.buscarComFiltroPorCompetencia(tipo, categoriaId, status, eventoId, de, ate);
    }

    /** `GET /admin/financeiro/contas` (M26 — recorte por `dataVencimento` sobre a mesma tabela,
     * ver ROADMAP.md § "Blueprint (M26)"). Ano/mês juntos viram uma janela
     * [1º dia do mês, 1º dia do mês seguinte); qualquer um dos dois nulo desliga o filtro de
     * período (mantém 100% do comportamento anterior de `ContaPagarReceberService.listar`). */
    public List<LancamentoFinanceiro> listarPorVencimento(TipoLancamento tipo, StatusLancamento status,
                                                            Integer ano, Integer mes, UUID eventoId) {
        LocalDate inicio = SEM_FILTRO_INICIO;
        LocalDate fim = SEM_FILTRO_FIM;
        if (ano != null && mes != null) {
            YearMonth periodo = YearMonth.of(ano, mes);
            inicio = periodo.atDay(1);
            fim = periodo.plusMonths(1).atDay(1);
        }
        return lancamentoRepository.buscarComFiltroPorVencimento(tipo, status, eventoId, inicio, fim);
    }

    @Transactional
    public LancamentoFinanceiro liquidar(UUID id, LiquidarLancamentoRequest request) {
        LancamentoFinanceiro lancamento = lancamentoRepository.buscarPorIdComEvento(id)
                .orElseThrow(() -> new IllegalArgumentException("Lançamento não encontrado."));
        lancamento.liquidar(request.dataPagamento());
        return lancamentoRepository.save(lancamento);
    }

    @Transactional
    public LancamentoFinanceiro liquidarParcial(UUID id, LiquidarParcialLancamentoRequest request) {
        LancamentoFinanceiro lancamento = lancamentoRepository.buscarPorIdComEvento(id)
                .orElseThrow(() -> new IllegalArgumentException("Lançamento não encontrado."));
        lancamento.liquidarParcial(request.valorPago(), request.dataPagamento());
        return lancamentoRepository.save(lancamento);
    }
}
