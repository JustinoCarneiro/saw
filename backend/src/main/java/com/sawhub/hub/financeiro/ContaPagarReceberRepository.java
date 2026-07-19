package com.sawhub.hub.financeiro;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContaPagarReceberRepository extends JpaRepository<ContaPagarReceber, UUID> {

    // LEFT JOIN FETCH em categoria (M21): achado ao vivo — ContaResponse nunca lia
    // conta.getCategoria(), então o LazyInitializationException fora da transação
    // (open-in-view=false) nunca disparava; ContaCsvService.exportar() é o primeiro consumidor de
    // ContaPagarReceberService.listar() a ler o nome da categoria, mesma classe de achado já
    // documentada no M12 (Mentoria.materiaisRecomendados).
    //
    // Query dinâmica única (change request 17/07/2026, item "filtro mensal"): substitui os 3
    // finders derivados que existiam antes (all/porTipo/porStatus) — mesmo padrão de parâmetro
    // nulável já usado em MentoradoRepository.buscarComFiltro, evita branch manual em Java pra
    // cada combinação de filtro (tipo x status x período já são 8 combinações).
    // inicio/fim são SEMPRE não-nulos aqui de propósito (ver ContaPagarReceberService.listar) —
    // tentar o mesmo idioma "(:param IS NULL OR ...)" usado pra tipo/status quebrou com
    // LocalDate: o Postgres não consegue inferir o tipo de um parâmetro nulo sem nenhum outro
    // contexto nesta query (tantas colunas bytea de pgcrypto que ele chuta bytea e explode
    // "cannot cast type bytea to date"). Em vez de brigar com o driver, o filtro "sem período"
    // vira uma faixa [1900-01-01, 2999-12-31) que cobre qualquer dataVencimento real — sentinela
    // sempre tipado, nunca null, sem ambiguidade nenhuma pro Postgres resolver.
    // LEFT JOIN FETCH em evento também (change request 17/07/2026, "evento no financeiro") —
    // mesmo raciocínio de categoria: ContaResponse agora lê c.getEvento().getTitulo().
    @Query("SELECT c FROM ContaPagarReceber c LEFT JOIN FETCH c.categoria LEFT JOIN FETCH c.evento "
            + "WHERE (:tipo IS NULL OR c.tipo = :tipo) "
            + "AND (:status IS NULL OR c.status = :status) "
            + "AND (:eventoId IS NULL OR c.evento.id = :eventoId) "
            + "AND c.dataVencimento >= :inicio "
            + "AND c.dataVencimento < :fim "
            + "ORDER BY c.dataVencimento ASC")
    List<ContaPagarReceber> buscarComFiltro(@Param("tipo") TipoConta tipo, @Param("status") StatusConta status,
                                             @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim,
                                             @Param("eventoId") UUID eventoId);

    // change request 17/07/2026 ("evento no financeiro") — liquidar()/liquidarParcial() precisam
    // ler conta.getEvento() pra propagar pro Lancamento gerado, e o ContaResponse devolvido pelo
    // controller lê evento.getTitulo() fora da transação original (open-in-view=false) — mesmo
    // raciocínio do JOIN FETCH de categoria em outros pontos deste repositório.
    @Query("SELECT c FROM ContaPagarReceber c LEFT JOIN FETCH c.categoria LEFT JOIN FETCH c.evento WHERE c.id = :id")
    Optional<ContaPagarReceber> buscarPorIdComEvento(@Param("id") UUID id);

    List<ContaPagarReceber> findByStatusAndDataVencimentoBefore(StatusConta status, LocalDate data);
}
