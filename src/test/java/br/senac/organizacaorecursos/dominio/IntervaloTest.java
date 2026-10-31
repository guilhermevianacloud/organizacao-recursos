package br.senac.organizacaorecursos.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Casos RN01-* e RN02-* de docs/testes/plano-tdd.md. Dia fixo 2026-11-09, UTC. */
class IntervaloTest {

    private static Instant as(String hora) {
        return Instant.parse("2026-11-09T" + hora + "Z");
    }

    @Nested
    @DisplayName("RN-01: o término deve ser posterior ao início")
    class TerminoPosteriorAoInicio {

        @Test
        void rn01_01_intervaloNormalEhAceito() {
            Intervalo intervalo = new Intervalo(as("08:00:00"), as("10:00:00"));

            assertThat(intervalo.inicio()).isEqualTo(as("08:00:00"));
            assertThat(intervalo.termino()).isEqualTo(as("10:00:00"));
        }

        @Test
        void rn01_02_duracaoDeUmSegundoEhAceita() {
            Intervalo intervalo = new Intervalo(as("08:00:00"), as("08:00:01"));

            assertThat(intervalo.termino()).isAfter(intervalo.inicio());
        }

        @Test
        void rn01_03_duracaoZeroEhRecusada() {
            assertThatThrownBy(() -> new Intervalo(as("08:00:00"), as("08:00:00")))
                    .isInstanceOf(IntervaloInvalidoException.class)
                    .extracting("codigo").isEqualTo("INTERVALO_INVALIDO");
        }

        @Test
        void rn01_04_terminoAntesDoInicioEhRecusado() {
            assertThatThrownBy(() -> new Intervalo(as("10:00:00"), as("08:00:00")))
                    .isInstanceOf(IntervaloInvalidoException.class);
        }

        @Test
        void rn01_05_inicioOuTerminoAusenteEhRecusado() {
            assertThatThrownBy(() -> new Intervalo(null, as("10:00:00")))
                    .isInstanceOf(IntervaloInvalidoException.class);
            assertThatThrownBy(() -> new Intervalo(as("08:00:00"), null))
                    .isInstanceOf(IntervaloInvalidoException.class);
        }
    }

    @Nested
    @DisplayName("RN-02: sobreposição com intervalo semiaberto [início, término) — ADR-004")
    class Sobreposicao {

        private final Intervalo reservaA = new Intervalo(as("08:00:00"), as("10:00:00"));

        @ParameterizedTest(name = "{0}: {1} a {2} sobrepõe 08:00–10:00? {3}")
        @CsvSource({
                "RN02-01, 10:00:00, 12:00:00, false",
                "RN02-02, 09:59:59, 12:00:00, true",
                "RN02-03, 10:00:01, 12:00:00, false",
                "RN02-04, 06:00:00, 08:00:00, false",
                "RN02-05, 06:00:00, 08:00:01, true",
                "RN02-06, 08:30:00, 09:30:00, true",
                "RN02-07, 07:00:00, 11:00:00, true",
                "mesmo horário, 08:00:00, 10:00:00, true"
        })
        void comparaComReservaDasOitoAsDez(String caso, String inicio, String termino, boolean esperado) {
            Intervalo novo = new Intervalo(as(inicio), as(termino));

            assertThat(reservaA.sobrepoe(novo)).as(caso).isEqualTo(esperado);
            assertThat(novo.sobrepoe(reservaA)).as(caso + " (simétrico)").isEqualTo(esperado);
        }
    }
}
