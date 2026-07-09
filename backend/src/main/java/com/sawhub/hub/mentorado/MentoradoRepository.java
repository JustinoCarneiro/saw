package com.sawhub.hub.mentorado;

import com.sawhub.hub.security.Usuario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MentoradoRepository extends JpaRepository<Mentorado, UUID> {
    Optional<Mentorado> findByUsuario(Usuario usuario);

    // Bug achado ao vivo (boot do M06): com :busca=null, o Postgres não conseguia inferir o tipo
    // do parâmetro dentro de CONCAT('%', :busca, '%') e escolhia `bytea` em vez de `text`
    // ("function lower(bytea) does not exist"). CAST(:busca AS string) força o tipo certo mesmo
    // quando o valor é null.
    @Query("SELECT m FROM Mentorado m "
            + "WHERE (:plano IS NULL OR m.plano = :plano) "
            + "AND (:status IS NULL OR m.status = :status) "
            + "AND (:busca IS NULL OR LOWER(m.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))) "
            + "ORDER BY m.nome ASC")
    List<Mentorado> buscarComFiltro(@Param("plano") Plano plano, @Param("status") StatusMentorado status,
                                     @Param("busca") String busca);
}
