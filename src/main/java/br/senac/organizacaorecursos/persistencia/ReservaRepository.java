package br.senac.organizacaorecursos.persistencia;

import br.senac.organizacaorecursos.dominio.EstadoReserva;
import br.senac.organizacaorecursos.dominio.Reserva;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
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

    /** Igual à anterior, ignorando a própria reserva (alteração de horário). */
    @Query("""
            select count(r) > 0 from Reserva r
            where r.recurso.id = :recursoId
              and r.id <> :ignorar
              and r.estado in :estados
              and r.inicio < :termino
              and :inicio < r.termino
            """)
    boolean existeSobreposicaoIgnorando(@Param("recursoId") Long recursoId,
                                        @Param("inicio") Instant inicio,
                                        @Param("termino") Instant termino,
                                        @Param("estados") Collection<EstadoReserva> estados,
                                        @Param("ignorar") Long ignorar);

    List<Reserva> findBySolicitanteIdOrderByInicioAsc(Long solicitanteId);

    /** Reservas que tocam [inicio, fim), em ordem de início. */
    @Query("select r from Reserva r where r.inicio < :fim and :inicio < r.termino order by r.inicio")
    List<Reserva> queTocam(@Param("inicio") Instant inicio, @Param("fim") Instant fim);

    List<Reserva> findByEstadoOrderByInicioAsc(EstadoReserva estado);
}
