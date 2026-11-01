package br.senac.organizacaorecursos.persistencia;

import br.senac.organizacaorecursos.dominio.Recurso;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecursoRepository extends JpaRepository<Recurso, Long> {

    /**
     * Busca o recurso travando a linha (SELECT ... FOR UPDATE) até o fim da transação.
     * Pedidos pelo mesmo recurso passam a ser atendidos um por vez: elimina o deadlock
     * entre inserções simultâneas na exclusion constraint (DEF-002, ADR-002).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Recurso r where r.id = :id")
    Optional<Recurso> buscarTravandoParaReserva(@Param("id") Long id);
}
