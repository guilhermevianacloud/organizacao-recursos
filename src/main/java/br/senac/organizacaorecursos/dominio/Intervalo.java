package br.senac.organizacaorecursos.dominio;

import java.time.Instant;

/**
 * Período ocupado por uma reserva ou manutenção, semiaberto: [início, término) — ADR-004.
 * Inclui o início e exclui o término, então reservas encostadas (8h–10h e 10h–12h) não conflitam.
 */
public record Intervalo(Instant inicio, Instant termino) {

    /** RN-01: o término deve ser posterior ao início. */
    public Intervalo {
        if (inicio == null || termino == null) {
            throw new IntervaloInvalidoException("Informe o início e o término.");
        }
        if (!termino.isAfter(inicio)) {
            throw new IntervaloInvalidoException("O término deve ser posterior ao início.");
        }
    }

    /** RN-02: dois intervalos se sobrepõem quando cada um começa antes de o outro terminar. */
    public boolean sobrepoe(Intervalo outro) {
        return inicio.isBefore(outro.termino) && outro.inicio.isBefore(termino);
    }
}
