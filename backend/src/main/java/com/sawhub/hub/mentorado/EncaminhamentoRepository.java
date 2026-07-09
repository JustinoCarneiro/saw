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
}
