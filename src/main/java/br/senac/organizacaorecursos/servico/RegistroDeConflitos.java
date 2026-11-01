package br.senac.organizacaorecursos.servico;

import br.senac.organizacaorecursos.dominio.Intervalo;
import java.sql.Timestamp;
import java.time.Clock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-12: guarda cada pedido recusado por sobreposição ("conflito evitado").
 * Transação própria (REQUIRES_NEW): o pedido recusado é desfeito, mas o registro da recusa fica.
 * É chamado pela API depois que a transação do pedido termina, para não disputar a trava do recurso.
 */
@Service
public class RegistroDeConflitos {

    private final JdbcTemplate jdbc;
    private final Clock relogio;

    public RegistroDeConflitos(JdbcTemplate jdbc, Clock relogio) {
        this.jdbc = jdbc;
        this.relogio = relogio;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Long recursoId, Intervalo pedido) {
        jdbc.update("insert into tentativa_recusada (recurso_id, inicio, termino, instante) values (?, ?, ?, ?)",
                recursoId, Timestamp.from(pedido.inicio()), Timestamp.from(pedido.termino()),
                Timestamp.from(relogio.instant()));
    }
}
