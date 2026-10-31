package br.senac.organizacaorecursos.persistencia;

import br.senac.organizacaorecursos.dominio.Recurso;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecursoRepository extends JpaRepository<Recurso, Long> {
}
