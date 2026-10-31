package br.senac.organizacaorecursos.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.senac.organizacaorecursos.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * A exclusion constraint (ADR-002) protege a regra mesmo quando a aplicação é contornada:
 * aqui as reservas são gravadas direto com SQL, sem passar pelo serviço.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ProtecaoNoBancoIT {

    @Autowired
    private JdbcTemplate jdbc;

    private long sala;
    private long solicitante;

    @BeforeEach
    void massa() {
        solicitante = jdbc.queryForObject(
                "insert into usuario (nome, email, perfil, senha_hash) "
                        + "values ('Teste SQL', 'sql@exemplo.invalid', 'SOLICITANTE', 'x') returning id", Long.class);
        sala = jdbc.queryForObject(
                "insert into recurso (tipo, nome, restrito) values ('SALA', 'Sala SQL', false) returning id", Long.class);
        inserirReserva("08:00", "10:00", "APROVADA");
    }

    @Test
    void bancoRecusaSobreposicaoMesmoSemPassarPelaAplicacao() {
        assertThatThrownBy(() -> inserirReserva("09:00", "11:00", "SOLICITADA"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(ServicoDeReservas.CONSTRAINT_SEM_SOBREPOSICAO);
    }

    @Test
    void bancoAceitaReservaEncostadaPorSerIntervaloSemiaberto() {
        inserirReserva("10:00", "12:00", "APROVADA");

        assertThat(contarReservas()).isEqualTo(2);
    }

    @Test
    void reservaCanceladaNaoOcupaORecursoNoBanco() {
        inserirReserva("08:00", "10:00", "CANCELADA");

        assertThat(contarReservas()).isEqualTo(2);
    }

    @Test
    void bancoRecusaTerminoAntesDoInicio() {
        assertThatThrownBy(() -> inserirReserva("12:00", "11:00", "APROVADA"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("reserva_termino_apos_inicio");
    }

    private void inserirReserva(String inicio, String termino, String estado) {
        jdbc.update("insert into reserva (recurso_id, solicitante_id, inicio, termino, estado) "
                        + "values (?, ?, ?::timestamptz, ?::timestamptz, ?)",
                sala, solicitante, "2026-11-09 " + inicio + ":00+00", "2026-11-09 " + termino + ":00+00", estado);
    }

    private int contarReservas() {
        Integer total = jdbc.queryForObject("select count(*) from reserva where recurso_id = ?", Integer.class, sala);
        return total == null ? 0 : total;
    }
}
