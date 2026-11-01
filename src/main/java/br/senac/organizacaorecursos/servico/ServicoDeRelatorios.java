package br.senac.organizacaorecursos.servico;

import br.senac.organizacaorecursos.dominio.EstadoReserva;
import br.senac.organizacaorecursos.dominio.Intervalo;
import br.senac.organizacaorecursos.dominio.Recurso;
import br.senac.organizacaorecursos.dominio.Reserva;
import br.senac.organizacaorecursos.dominio.TipoRecurso;
import br.senac.organizacaorecursos.persistencia.ReservaRepository;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-12: horas reservadas por recurso, carga horária por professor e conflitos evitados no período.
 * Conta como uso só reserva APROVADA, EM_USO ou CONCLUIDA (RN-11: cancelada, rejeitada, não aprovada e
 * não comparecimento ficam de fora). Reserva que cruza a borda conta só as horas dentro do período (D-14).
 */
@Service
public class ServicoDeRelatorios {

    static final Set<EstadoReserva> ESTADOS_DE_USO =
            EnumSet.of(EstadoReserva.APROVADA, EstadoReserva.EM_USO, EstadoReserva.CONCLUIDA);

    private final ReservaRepository reservas;
    private final JdbcTemplate jdbc;

    public ServicoDeRelatorios(ReservaRepository reservas, JdbcTemplate jdbc) {
        this.reservas = reservas;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public Relatorio utilizacao(Intervalo periodo) {
        Map<Recurso, Long> segundosPorRecurso = new LinkedHashMap<>();
        for (Reserva reserva : reservas.queTocam(periodo.inicio(), periodo.termino())) {
            if (!ESTADOS_DE_USO.contains(reserva.getEstado())) {
                continue;
            }
            Instant inicio = max(reserva.intervalo().inicio(), periodo.inicio());
            Instant fim = min(reserva.intervalo().termino(), periodo.termino());
            segundosPorRecurso.merge(reserva.getRecurso(), Duration.between(inicio, fim).toSeconds(), Long::sum);
        }
        List<Uso> recursos = segundosPorRecurso.entrySet().stream()
                .map(e -> new Uso(e.getKey().getId(), e.getKey().getNome(), e.getKey().getTipo(), horas(e.getValue())))
                .sorted(Comparator.comparing(Uso::nome))
                .toList();
        List<Uso> professores = recursos.stream().filter(u -> u.tipo() == TipoRecurso.PROFESSOR).toList();
        Long conflitos = jdbc.queryForObject(
                "select count(*) from tentativa_recusada where inicio < ? and ? < termino", Long.class,
                Timestamp.from(periodo.termino()), Timestamp.from(periodo.inicio()));
        return new Relatorio(periodo.inicio(), periodo.termino(), recursos, professores, conflitos == null ? 0 : conflitos);
    }

    private static double horas(long segundos) {
        return Math.round(segundos / 36.0) / 100.0;
    }

    private static Instant max(Instant a, Instant b) {
        return a.isAfter(b) ? a : b;
    }

    private static Instant min(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }

    public record Uso(Long recursoId, String nome, TipoRecurso tipo, double horas) {
    }

    public record Relatorio(Instant inicio, Instant fim, List<Uso> recursos, List<Uso> professores,
                            long conflitosEvitados) {
    }
}
