package com.sawhub.hub.mentorado;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EncaminhamentoRepository extends JpaRepository<Encaminhamento, UUID> {
    // M08 — Dashboard do Mentorado, mantido como estava (usa isConcluido() derivado agora).
    List<Encaminhamento> findByMentoradoIdOrderByCriadoEmDesc(UUID mentoradoId);

    // M10 — achado ao vivo: findById() puro deixa `meta` como proxy LAZY não inicializado; usado
    // por atualizar()/avancarStatus(), cujo TarefaResponse (fora da transação) lê o título da
    // meta vinculada e quebrava com LazyInitializationException. Mesma classe de bug já corrigida
    // em MentoriaRepository.buscarPorIdComDetalhes (M06).
    @Query("SELECT e FROM Encaminhamento e LEFT JOIN FETCH e.meta WHERE e.id = :id")
    Optional<Encaminhamento> buscarPorIdComMeta(@Param("id") UUID id);

    // M10 — listagem self-service (H4.1/H4.3). LEFT JOIN FETCH em meta: é LAZY e o TarefaResponse
    // (fora da transação, open-in-view=false) lê o título dela — mesmo achado do
    // LazyInitializationException já corrigido em outros repositórios (LeadRepository, M05).
    // :busca usa CAST(... AS string) — bug já achado ao vivo no M06 (MentoradoRepository) quando o
    // parâmetro é null e o Postgres não consegue inferir o tipo dentro do LIKE/CONCAT.
    @Query("SELECT e FROM Encaminhamento e LEFT JOIN FETCH e.meta "
            + "WHERE e.mentorado.id = :mentoradoId "
            + "AND (:status IS NULL OR e.status = :status) "
            + "AND (:busca IS NULL OR LOWER(e.titulo) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))) "
            + "ORDER BY e.criadoEm DESC")
    List<Encaminhamento> buscarPorMentorado(@Param("mentoradoId") UUID mentoradoId,
                                             @Param("status") StatusTarefa status,
                                             @Param("busca") String busca);

    // Fase 5 (H4.6) — edição/status pelo Admin (EncaminhamentoAdminService): findById() puro deixa
    // `mentorado` como proxy LAZY não inicializado; EncaminhamentoAdminResponse (fora da transação,
    // open-in-view=false) lê nome/id do mentorado e quebraria com LazyInitializationException —
    // mesma classe de bug já corrigida em buscarPorIdComMeta (self-service) acima.
    @Query("SELECT e FROM Encaminhamento e LEFT JOIN FETCH e.mentorado WHERE e.id = :id")
    Optional<Encaminhamento> buscarPorIdComMentorado(@Param("id") UUID id);

    // Fase 5 — mesmo achado do MetaRepository.listarTodasComMentorado(): findAll() (usado por
    // EncaminhamentoCsvService.exportar() e pela listagem admin nova) deixava `mentorado`/
    // `mentorado.usuario` LAZY não inicializado fora de transação — GET /admin/encaminhamentos/
    // export retornava 500 em produção, nunca coberto por E2E.
    @Query("SELECT e FROM Encaminhamento e LEFT JOIN FETCH e.mentorado men LEFT JOIN FETCH men.usuario "
            + "ORDER BY e.criadoEm DESC")
    List<Encaminhamento> listarTodasComMentorado();
}
