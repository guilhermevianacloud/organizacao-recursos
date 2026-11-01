package br.senac.organizacaorecursos.servico;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.senac.organizacaorecursos.dominio.AcessoNegadoException;
import br.senac.organizacaorecursos.dominio.ConflitoDeHorarioException;
import br.senac.organizacaorecursos.dominio.Intervalo;
import br.senac.organizacaorecursos.dominio.Recurso;
import br.senac.organizacaorecursos.dominio.Reserva;
import br.senac.organizacaorecursos.dominio.TipoRecurso;
import br.senac.organizacaorecursos.persistencia.AuditoriaReservaRepository;
import br.senac.organizacaorecursos.persistencia.ManutencaoRepository;
import br.senac.organizacaorecursos.persistencia.RecursoRepository;
import br.senac.organizacaorecursos.persistencia.ReservaRepository;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Teste de unidade isolado, o único lugar onde a especificação permite mock (Mockito): exercita caminhos de
 * defesa que o banco real não alcança com a trava ligada (lacunas de risco apontadas pelo JaCoCo).
 */
class ServicoDeReservasTest {

    private static final Intervalo OITO_AS_DEZ =
            new Intervalo(Instant.parse("2026-11-09T08:00:00Z"), Instant.parse("2026-11-09T10:00:00Z"));

    private final RecursoRepository recursos = mock(RecursoRepository.class);
    private final ReservaRepository reservas = mock(ReservaRepository.class);
    private final ManutencaoRepository manutencoes = mock(ManutencaoRepository.class);
    private final AuditoriaReservaRepository auditoria = mock(AuditoriaReservaRepository.class);
    private final Clock relogio = Clock.fixed(Instant.parse("2026-11-01T12:00:00Z"), ZoneOffset.UTC);
    private ServicoDeReservas servico;
    private final Recurso sala = new Recurso(TipoRecurso.SALA, "Sala 101", false);

    @BeforeEach
    void montar() {
        servico = new ServicoDeReservas(recursos, reservas, manutencoes, auditoria, relogio,
                mock(ApplicationEventPublisher.class));
        when(recursos.buscarTravandoParaReserva(anyLong())).thenReturn(Optional.of(sala));
    }

    /** Se a trava sumir e a constraint do banco recusar, o usuário recebe conflito (409), não erro 500. */
    @Test
    void violacaoDaConstraintDeSobreposicaoViraConflitoDeHorario() {
        when(reservas.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("could not execute statement",
                new SQLException("ERROR: conflicting key value violates exclusion constraint \"reserva_sem_sobreposicao\"")));

        assertThatThrownBy(() -> servico.solicitar(1L, 7L, OITO_AS_DEZ))
                .isInstanceOf(ConflitoDeHorarioException.class);
    }

    /** Outra violação de integridade não é disfarçada de conflito: sobe como está (e vira 500 genérico na API). */
    @Test
    void outraViolacaoDeIntegridadeNaoEhDisfarcadaDeConflito() {
        when(reservas.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("could not execute statement",
                new SQLException("ERROR: insert or update violates foreign key constraint \"reserva_solicitante_id_fkey\"")));

        assertThatThrownBy(() -> servico.solicitar(1L, 7L, OITO_AS_DEZ))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /** RN-06 dentro do serviço: mesmo que o perfil passe, quem pediu não aprova a própria reserva. */
    @Test
    void rn06_quemPediuNaoAprovaAPropriaReservaNoServico() {
        Recurso laboratorio = new Recurso(TipoRecurso.SALA, "Laboratório", true);
        Reserva reserva = Reserva.solicitar(laboratorio, 7L, OITO_AS_DEZ);
        when(reservas.findById(5L)).thenReturn(Optional.of(reserva));
        when(reservas.existeSobreposicao(anyLong(), any(), any(), anyCollection())).thenReturn(false);

        assertThatThrownBy(() -> servico.aprovar(5L, 7L))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessageContaining("não pode aprová-la");
    }
}
