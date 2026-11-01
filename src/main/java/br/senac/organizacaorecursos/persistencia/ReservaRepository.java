package br.senac.organizacaorecursos.persistencia;

import br.senac.organizacaorecursos.dominio.EstadoReserva;
import br.senac.organizacaorecursos.dominio.Reserva;
import java.time.Instant;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /** Existe reserva nos estados dados que cruza [inicio, termino)? Mesmo critério do ADR-004. */
    @Query("""
            select count(r) > 0 from Reserva r
            where r.recurso.id = :recursoId
              and r.estado in :estados
              and r.inicio < :termino
              and :inicio < r.termino
            """)
    boolean existeSobreposicao(@Param("recursoId") Long recursoId,
                               @Param("inicio") Instant inicio,
                               @Param("termino") Instant termino,
                               @Param("estados") Collection<EstadoReserva> estados);
}
