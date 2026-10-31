package br.senac.organizacaorecursos.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/** RN-07 (transições) e RN-08 (reserva iniciada). Tabela de decisão completa: 7 × 7 = 49 pares. */
class EstadoReservaTest {

    /** Transições permitidas, copiadas do PRD (seção 8.1) — escritas aqui de forma independente do código. */
    private static final Set<String> PERMITIDAS = Set.of(
            "SOLICITADA>APROVADA",
            "SOLICITADA>REJEITADA",
            "SOLICITADA>CANCELADA",
            "APROVADA>CANCELADA",
            "APROVADA>EM_USO",
            "APROVADA>NAO_COMPARECEU",
            "EM_USO>CONCLUIDA");

    static Stream<Arguments> todosOsPares() {
        return Arrays.stream(EstadoReserva.values())
                .flatMap(de -> Arrays.stream(EstadoReserva.values())
                        .map(para -> Arguments.of(de, para, PERMITIDAS.contains(de + ">" + para))));
    }

    @ParameterizedTest(name = "RN-07: {0} → {1} permitida? {2}")
    @MethodSource("todosOsPares")
    void soAsTransicoesDoPrdSaoPermitidas(EstadoReserva de, EstadoReserva para, boolean permitida) {
        assertThat(de.podeTransitarPara(para)).isEqualTo(permitida);
    }

    @ParameterizedTest(name = "{0} ocupa o recurso? {1}")
    @CsvSource({
            "SOLICITADA, true", "APROVADA, true", "EM_USO, true",
            "CONCLUIDA, false", "REJEITADA, false", "CANCELADA, false", "NAO_COMPARECEU, false"
    })
    void soEstadosAtivosOcupamORecurso(EstadoReserva estado, boolean ocupa) {
        assertThat(estado.ocupaRecurso()).isEqualTo(ocupa);
    }

    @ParameterizedTest(name = "RN-08: {0} é iniciada? {1}")
    @CsvSource({
            "SOLICITADA, false", "APROVADA, false", "REJEITADA, false", "CANCELADA, false",
            "EM_USO, true", "CONCLUIDA, true", "NAO_COMPARECEU, true"
    })
    void reservaIniciadaEhDefinidaPeloEstado(EstadoReserva estado, boolean iniciada) {
        assertThat(estado.iniciada()).isEqualTo(iniciada);
    }
}
