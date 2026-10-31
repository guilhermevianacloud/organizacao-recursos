package br.senac.organizacaorecursos.dominio;

import java.time.Instant;

/** Período ocupado por uma reserva ou manutenção. */
public record Intervalo(Instant inicio, Instant termino) {

    public boolean sobrepoe(Intervalo outro) {
        return false;
    }
}
