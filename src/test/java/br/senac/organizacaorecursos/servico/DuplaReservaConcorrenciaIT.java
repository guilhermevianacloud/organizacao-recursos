package br.senac.organizacaorecursos.servico;

import static org.assertj.core.api.Assertions.assertThat;

import br.senac.organizacaorecursos.TestcontainersConfiguration;
import br.senac.organizacaorecursos.dominio.ConflitoDeHorarioException;
import br.senac.organizacaorecursos.dominio.Intervalo;
import br.senac.organizacaorecursos.dominio.Perfil;
import br.senac.organizacaorecursos.dominio.Recurso;
import br.senac.organizacaorecursos.dominio.TipoRecurso;
import br.senac.organizacaorecursos.dominio.Usuario;
import br.senac.organizacaorecursos.persistencia.RecursoRepository;
import br.senac.organizacaorecursos.persistencia.UsuarioRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * RN-03 / RF-06 (limitador de nota): 20 pedidos simultâneos pelo mesmo recurso e horário
 * devem produzir exatamente uma reserva aceita. Roteiro da Aula 10: ExecutorService + CountDownLatch,
 * PostgreSQL real, contagem conferida por consulta ao banco e execução repetida.
 * Sem @Transactional: cada pedido precisa da própria transação, como em produção.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DuplaReservaConcorrenciaIT {

    private static final int PEDIDOS = 20;
    private static final Intervalo OITO_AS_DEZ =
            new Intervalo(Instant.parse("2026-11-09T08:00:00Z"), Instant.parse("2026-11-09T10:00:00Z"));

    @Autowired
    private ServicoDeReservas servicoDeReservas;
    @Autowired
    private RecursoRepository recursos;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private JdbcTemplate jdbc;

    private Long solicitante;

    @BeforeEach
    void bancoLimpo() {
        jdbc.execute("TRUNCATE reserva, manutencao, recurso, usuario RESTART IDENTITY CASCADE");
        solicitante = usuarios.save(
                new Usuario("Marina Teste", "marina@exemplo.invalid", Perfil.SOLICITANTE, "hash-ficticio")).getId();
    }

    @RepeatedTest(value = 5, name = "RN03-01/03: sala, execução {currentRepetition} de {totalRepetitions}")
    void vintePedidosSimultaneosPelaMesmaSalaGeramUmaUnicaReserva() throws Exception {
        Recurso sala = recursos.save(new Recurso(TipoRecurso.SALA, "Sala 101", false));

        Resultado resultado = dispararAoMesmoTempo(i -> sala.getId());

        assertThat(resultado.inesperadas()).as("exceções inesperadas").isEmpty();
        assertThat(resultado.aceitas()).as("pedidos aceitos").isEqualTo(1);
        assertThat(resultado.conflitos()).as("pedidos recusados por conflito").isEqualTo(PEDIDOS - 1);
        assertThat(reservasAtivasNoBanco(sala.getId())).as("linhas no banco").isEqualTo(1);
    }

    @Test
    void rn03_02_vintePedidosSimultaneosPeloMesmoProfessorGeramUmaUnicaReserva() throws Exception {
        Recurso professor = recursos.save(new Recurso(TipoRecurso.PROFESSOR, "Prof. Ana Teste", false));

        Resultado resultado = dispararAoMesmoTempo(i -> professor.getId());

        assertThat(resultado.inesperadas()).isEmpty();
        assertThat(resultado.aceitas()).isEqualTo(1);
        assertThat(resultado.conflitos()).isEqualTo(PEDIDOS - 1);
        assertThat(reservasAtivasNoBanco(professor.getId())).isEqualTo(1);
    }

    @Test
    void rn03_04_vintePedidosSimultaneosParaVinteSalasDiferentesSaoTodosAceitos() throws Exception {
        List<Long> salas = new ArrayList<>();
        for (int i = 1; i <= PEDIDOS; i++) {
            salas.add(recursos.save(new Recurso(TipoRecurso.SALA, "Sala " + (200 + i), false)).getId());
        }

        Resultado resultado = dispararAoMesmoTempo(salas::get);

        assertThat(resultado.inesperadas()).isEmpty();
        assertThat(resultado.aceitas()).isEqualTo(PEDIDOS);
        assertThat(resultado.conflitos()).isZero();
    }

    /** Prepara as 20 threads, solta todas juntas e conta o que aconteceu com cada pedido. */
    private Resultado dispararAoMesmoTempo(IntFunction<Long> recursoDoPedido) throws Exception {
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger aceitas = new AtomicInteger();
        AtomicInteger conflitos = new AtomicInteger();
        ConcurrentLinkedQueue<Throwable> inesperadas = new ConcurrentLinkedQueue<>();
        ExecutorService executor = Executors.newFixedThreadPool(PEDIDOS);
        try {
            List<Future<?>> pedidos = new ArrayList<>();
            for (int i = 0; i < PEDIDOS; i++) {
                Long recursoId = recursoDoPedido.apply(i);
                pedidos.add(executor.submit(() -> {
                    largada.await();
                    try {
                        servicoDeReservas.solicitar(recursoId, solicitante, OITO_AS_DEZ);
                        aceitas.incrementAndGet();
                    } catch (ConflitoDeHorarioException e) {
                        conflitos.incrementAndGet();
                    } catch (RuntimeException e) {
                        inesperadas.add(e);
                    }
                    return null;
                }));
            }
            largada.countDown();
            for (Future<?> pedido : pedidos) {
                pedido.get(60, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
        return new Resultado(aceitas.get(), conflitos.get(), List.copyOf(inesperadas));
    }

    private int reservasAtivasNoBanco(Long recursoId) {
        Integer total = jdbc.queryForObject(
                "select count(*) from reserva where recurso_id = ? and estado in ('SOLICITADA', 'APROVADA', 'EM_USO')",
                Integer.class, recursoId);
        return total == null ? 0 : total;
    }

    private record Resultado(int aceitas, int conflitos, List<Throwable> inesperadas) {
    }
}
