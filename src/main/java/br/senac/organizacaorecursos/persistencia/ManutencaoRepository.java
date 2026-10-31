package br.senac.organizacaorecursos.persistencia;

import br.senac.organizacaorecursos.dominio.Manutencao;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ManutencaoRepository extends JpaRepository<Manutencao, Long> {

    /** Existe manutenção do recurso que cruza [inicio, termino)? */
    @Query("""
            select count(m) > 0 from Manutencao m
            where m.recurso.id = :recursoId
              and m.inicio < :termino
              and :inicio < m.termino
            """)
    boolean existeSobreposicao(@Param("recursoId") Long recursoId,
                               @Param("inicio") Instant inicio,
                               @Param("termino") Instant termino);
}
