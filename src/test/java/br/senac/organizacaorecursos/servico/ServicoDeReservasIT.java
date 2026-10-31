package br.senac.organizacaorecursos.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.senac.organizacaorecursos.TestcontainersConfiguration;
import br.senac.organizacaorecursos.dominio.ConflitoDeHorarioException;
import br.senac.organizacaorecursos.dominio.EstadoReserva;
import br.senac.organizacaorecursos.dominio.Intervalo;
import br.senac.organizacaorecursos.dominio.ManutencaoConflitaComReservaException;
import br.senac.organizacaorecursos.dominio.Perfil;
import br.senac.organizacaorecursos.dominio.Recurso;
import br.senac.organizacaorecursos.dominio.RecursoEmManutencaoException;
import br.senac.organizacaorecursos.dominio.RecursoNaoEncontradoException;
import br.senac.organizacaorecursos.dominio.Reserva;
import br.senac.organizacaorecursos.dominio.TipoRecurso;
import br.senac.organizacaorecursos.dominio.Usuario;
import br.senac.organizacaorecursos.persistencia.RecursoRepository;
import br.senac.organizacaorecursos.persistencia.UsuarioRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * RN-02 e RN-04 contra PostgreSQL real (Testcontainers), um pedido por vez.
 * Cada teste roda numa transação desfeita no fim: os casos não interferem entre si.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ServicoDeReservasIT {

    @Autowired
    private ServicoDeReservas servicoDeReservas;
    @Autowired
    private ServicoDeManutencao servicoDeManutencao;
    @Autowired
    private RecursoRepository recursos;
    @Autowired
    private UsuarioRepository usuarios;

    private Long solicitante;
    private Recurso sala101;
    private Recurso sala102;

    private static Intervalo das(String inicio, String termino) {
        return new Intervalo(Instant.parse("2026-11-09T" + inicio + "Z"), Instant.parse("2026-11-09T" + termino + "Z"));
    }

    @BeforeEach
    void prepararMassa() {
        solicitante = usuarios.save(
                new Usuario("Marina Teste", "marina@exemplo.invalid", Perfil.SOLICITANTE, "hash-ficticio")).getId();
        sala101 = recursos.save(new Recurso(TipoRecurso.SALA, "Sala 101", false));
        sala102 = recursos.save(new Recurso(TipoRecurso.SALA, "Sala 102", false));
    }

    @Nested
    @DisplayName("RN-02: sem sobreposição de reservas ativas do mesmo recurso")
    class Sobreposicao {

        @BeforeEach
        void reservaDasOitoAsDez() {
            servicoDeReservas.solicitar(sala101.getId(), solicitante, das("08:00:00", "10:00:00"));
        }

        @Test
        void rn02_01_reservaEncostadaNoFimEhAceita() {
            Reserva reserva = servicoDeReservas.solicitar(sala101.getId(), solicitante, das("10:00:00", "12:00:00"));

            assertThat(reserva.getId()).isNotNull();
            assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.APROVADA);
        }

        @Test
        void rn02_02_reservaQueCruzaUmSegundoEhRecusada() {
            assertThatThrownBy(() -> servicoDeReservas.solicitar(sala101.getId(), solicitante, das("09:59:59", "12:00:00")))
                    .isInstanceOf(ConflitoDeHorarioException.class)
                    .extracting("codigo").isEqualTo("CONFLITO_DE_HORARIO");
        }

        @Test
        void rn02_06_reservaDentroDeOutraEhRecusada() {
            assertThatThrownBy(() -> servicoDeReservas.solicitar(sala101.getId(), solicitante, das("08:30:00", "09:30:00")))
                    .isInstanceOf(ConflitoDeHorarioException.class);
        }

        @Test
        void rn02_08_outroRecursoNoMesmoHorarioEhAceito() {
            Reserva reserva = servicoDeReservas.solicitar(sala102.getId(), solicitante, das("08:00:00", "10:00:00"));

            assertThat(reserva.getId()).isNotNull();
        }

        @Test
        void rn02_09_reservaCanceladaLiberaOHorario() {
            Reserva outra = servicoDeReservas.solicitar(sala102.getId(), solicitante, das("13:00:00", "14:00:00"));
            servicoDeReservas.cancelar(outra.getId(), solicitante);

            Reserva nova = servicoDeReservas.solicitar(sala102.getId(), solicitante, das("13:00:00", "14:00:00"));

            assertThat(nova.getEstado()).isEqualTo(EstadoReserva.APROVADA);
        }

        @Test
        void rn02_10_agendaDoProfessorTambemNaoSobrepoe() {
            Recurso professor = recursos.save(new Recurso(TipoRecurso.PROFESSOR, "Prof. Ana Teste", false));
            servicoDeReservas.solicitar(professor.getId(), solicitante, das("08:00:00", "10:00:00"));

            assertThatThrownBy(() -> servicoDeReservas.solicitar(professor.getId(), solicitante, das("09:00:00", "11:00:00")))
                    .isInstanceOf(ConflitoDeHorarioException.class);
        }

        @Test
        void rn02_11_materialTambemNaoSobrepoe() {
            Recurso projetor = recursos.save(new Recurso(TipoRecurso.MATERIAL, "Projetor 7", false));
            servicoDeReservas.solicitar(projetor.getId(), solicitante, das("08:00:00", "10:00:00"));

            assertThatThrownBy(() -> servicoDeReservas.solicitar(projetor.getId(), solicitante, das("08:00:00", "10:00:00")))
                    .isInstanceOf(ConflitoDeHorarioException.class);
        }
    }

    @Nested
    @DisplayName("RN-04: recurso em manutenção não pode ser reservado (manutenção das 13h às 17h)")
    class EmManutencao {

        @BeforeEach
        void bloquearSala101() {
            servicoDeManutencao.bloquear(sala101.getId(), das("13:00:00", "17:00:00"), "troca do ar-condicionado");
        }

        @Test
        void rn04_01_reservaQueTerminaNoInicioDaManutencaoEhAceita() {
            assertThat(servicoDeReservas.solicitar(sala101.getId(), solicitante, das("11:00:00", "13:00:00")).getId())
                    .isNotNull();
        }

        @Test
        void rn04_02_reservaQueCruzaUmSegundoEhRecusada() {
            assertThatThrownBy(() -> servicoDeReservas.solicitar(sala101.getId(), solicitante, das("11:00:00", "13:00:01")))
                    .isInstanceOf(RecursoEmManutencaoException.class)
                    .extracting("codigo").isEqualTo("RECURSO_EM_MANUTENCAO");
        }

        @Test
        void rn04_03_reservaQueComecaNoFimDaManutencaoEhAceita() {
            assertThat(servicoDeReservas.solicitar(sala101.getId(), solicitante, das("17:00:00", "18:00:00")).getId())
                    .isNotNull();
        }

        @Test
        void rn04_04_reservaQueComecaUmSegundoAntesDoFimEhRecusada() {
            assertThatThrownBy(() -> servicoDeReservas.solicitar(sala101.getId(), solicitante, das("16:59:59", "18:00:00")))
                    .isInstanceOf(RecursoEmManutencaoException.class);
        }

        @Test
        void rn04_05_reservaDentroDaManutencaoEhRecusada() {
            assertThatThrownBy(() -> servicoDeReservas.solicitar(sala101.getId(), solicitante, das("14:00:00", "15:00:00")))
                    .isInstanceOf(RecursoEmManutencaoException.class);
        }

        @Test
        void rn04_06_outraSalaNoMesmoHorarioEhAceita() {
            assertThat(servicoDeReservas.solicitar(sala102.getId(), solicitante, das("14:00:00", "15:00:00")).getId())
                    .isNotNull();
        }

        @Test
        void rn04_07_manutencaoSobreReservaAtivaEhRecusada() {
            servicoDeReservas.solicitar(sala102.getId(), solicitante, das("09:00:00", "10:00:00"));

            assertThatThrownBy(() -> servicoDeManutencao.bloquear(sala102.getId(), das("08:00:00", "12:00:00"), "pintura"))
                    .isInstanceOf(ManutencaoConflitaComReservaException.class);
        }

        @Test
        void manutencaoSobreReservaCanceladaEhAceita() {
            Reserva reserva = servicoDeReservas.solicitar(sala102.getId(), solicitante, das("09:00:00", "10:00:00"));
            servicoDeReservas.cancelar(reserva.getId(), solicitante);

            assertThat(servicoDeManutencao.bloquear(sala102.getId(), das("08:00:00", "12:00:00"), "pintura").getId())
                    .isNotNull();
        }
    }

    @Test
    void recursoInexistenteEhInformado() {
        assertThatThrownBy(() -> servicoDeReservas.solicitar(999_999L, solicitante, das("08:00:00", "10:00:00")))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
