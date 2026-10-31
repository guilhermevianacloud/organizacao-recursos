package br.senac.organizacaorecursos.dominio;

/** Estados da reserva (especificação): SOLICITADA → APROVADA → EM_USO → CONCLUIDA, e os alternativos. */
public enum EstadoReserva {
    SOLICITADA,
    APROVADA,
    EM_USO,
    CONCLUIDA,
    REJEITADA,
    CANCELADA,
    NAO_COMPARECEU;

    public boolean podeTransitarPara(EstadoReserva novo) {
        return true;
    }

    public boolean ocupaRecurso() {
        return true;
    }

    public boolean iniciada() {
        return false;
    }
}
