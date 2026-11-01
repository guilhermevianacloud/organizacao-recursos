package br.senac.organizacaorecursos.persistencia;

import br.senac.organizacaorecursos.dominio.AuditoriaReserva;
import java.util.List;
import org.springframework.data.repository.Repository;

/** Só inclui e lê: a interface não oferece alteração nem exclusão (RN-09). */
public interface AuditoriaReservaRepository extends Repository<AuditoriaReserva, Long> {

    AuditoriaReserva save(AuditoriaReserva registro);

    List<AuditoriaReserva> findByReservaIdOrderByIdAsc(Long reservaId);
}
