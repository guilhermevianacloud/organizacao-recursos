package br.senac.organizacaorecursos.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Comportamento da reserva: estado inicial (D-08), transições (RN-07) e exclusão (RN-08). */
class ReservaTest {

    private static final Long SOLICITANTE = 1L;
    private static final Intervalo OITO_AS_DEZ =
            new Intervalo(Instant.parse("2026-11-09T08:00:00Z"), Instant.parse("2026-11-09T10:00:00Z"));

    private final Recurso laboratorioRestrito = new Recurso(TipoRecurso.SALA, "Laboratório 3", true);
    private final Recurso sala101 = new Recurso(TipoRecurso.SALA, "Sala 101", false);

    @Test
    void rn05_05_recursoNaoRestritoNasceAprovado() {
        Reserva reserva = Reserva.solicitar(sala101, SOLICITANTE, OITO_AS_DEZ);

        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.APROVADA);
    }

    @Test
    void recursoRestritoNasceSolicitadaEAguardaResponsavel() {
        Reserva reserva = Reserva.solicitar(laboratorioRestrito, SOLICITANTE, OITO_AS_DEZ);

        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.SOLICITADA);
    }

    @Test
    void rn07_08_transicaoProibidaEhRecusadaENaoMudaOEstado() {
        Reserva reserva = Reserva.solicitar(laboratorioRestrito, SOLICITANTE, OITO_AS_DEZ);

        assertThatThrownBy(() -> reserva.transitarPara(EstadoReserva.EM_USO))
                .isInstanceOf(TransicaoInvalidaException.class)
                .extracting("codigo").isEqualTo("TRANSICAO_INVALIDA");
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.SOLICITADA);
    }

    @Test
    void rn07_09_reservaCanceladaNaoVoltaAFicarAprovada() {
        Reserva reserva = Reserva.solicitar(sala101, SOLICITANTE, OITO_AS_DEZ);
        reserva.transitarPara(EstadoReserva.CANCELADA);

        assertThatThrownBy(() -> reserva.transitarPara(EstadoReserva.APROVADA))
                .isInstanceOf(TransicaoInvalidaException.class);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
    }

    @Test
    void transicaoPermitidaMudaOEstado() {
        Reserva reserva = Reserva.solicitar(sala101, SOLICITANTE, OITO_AS_DEZ);

        reserva.transitarPara(EstadoReserva.EM_USO);

        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.EM_USO);
    }

    @ParameterizedTest(name = "RN-08: reserva em {0} não pode ser apagada")
    @EnumSource(value = EstadoReserva.class, names = {"EM_USO", "CONCLUIDA", "NAO_COMPARECEU"})
    void rn08_reservaIniciadaNaoPodeSerApagada(EstadoReserva estadoIniciado) {
        Reserva reserva = Reserva.solicitar(sala101, SOLICITANTE, OITO_AS_DEZ);
        levarAte(reserva, estadoIniciado);

        assertThat(reserva.podeSerApagada()).isFalse();
    }

    @Test
    void reservaAindaNaoIniciadaPodeSerApagada() {
        Reserva reserva = Reserva.solicitar(laboratorioRestrito, SOLICITANTE, OITO_AS_DEZ);

        assertThat(reserva.podeSerApagada()).isTrue();
    }

    private static final Intervalo DEZ_AS_ONZE =
            new Intervalo(Instant.parse("2026-11-09T10:00:00Z"), Instant.parse("2026-11-09T11:00:00Z"));

    @Test
    void alterarHorarioDeReservaAprovadaDeRecursoNaoRestritoMudaOIntervalo() {
        Reserva reserva = Reserva.solicitar(sala101, SOLICITANTE, OITO_AS_DEZ);

        reserva.alterarHorario(DEZ_AS_ONZE);

        assertThat(reserva.intervalo()).isEqualTo(DEZ_AS_ONZE);
    }

    @Test
    void alterarHorarioDeRestritoAindaSolicitadoEhPermitido() {
        Reserva reserva = Reserva.solicitar(laboratorioRestrito, SOLICITANTE, OITO_AS_DEZ);

        reserva.alterarHorario(DEZ_AS_ONZE);

        assertThat(reserva.intervalo()).isEqualTo(DEZ_AS_ONZE);
    }

    /** A aprovação valeu para o horário original: restrito já aprovado não muda de horário. */
    @Test
    void alterarHorarioDeRestritoJaAprovadoEhRecusado() {
        Reserva reserva = Reserva.solicitar(laboratorioRestrito, SOLICITANTE, OITO_AS_DEZ);
        reserva.transitarPara(EstadoReserva.APROVADA);

        assertThatThrownBy(() -> reserva.alterarHorario(DEZ_AS_ONZE))
                .isInstanceOf(RegraDeNegocioException.class)
                .extracting("codigo").isEqualTo("ALTERACAO_NAO_PERMITIDA");
        assertThat(reserva.intervalo()).isEqualTo(OITO_AS_DEZ);
    }

    @ParameterizedTest(name = "reserva em {0} não muda de horário")
    @EnumSource(value = EstadoReserva.class, names = {"EM_USO", "CONCLUIDA", "NAO_COMPARECEU"})
    void alterarHorarioDeReservaIniciadaEhRecusado(EstadoReserva estadoIniciado) {
        Reserva reserva = Reserva.solicitar(sala101, SOLICITANTE, OITO_AS_DEZ);
        levarAte(reserva, estadoIniciado);

        assertThatThrownBy(() -> reserva.alterarHorario(DEZ_AS_ONZE))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    private static void levarAte(Reserva reserva, EstadoReserva alvo) {
        switch (alvo) {
            case EM_USO -> reserva.transitarPara(EstadoReserva.EM_USO);
            case CONCLUIDA -> {
                reserva.transitarPara(EstadoReserva.EM_USO);
                reserva.transitarPara(EstadoReserva.CONCLUIDA);
            }
            case NAO_COMPARECEU -> reserva.transitarPara(EstadoReserva.NAO_COMPARECEU);
            default -> throw new IllegalArgumentException(alvo.name());
        }
    }
}
