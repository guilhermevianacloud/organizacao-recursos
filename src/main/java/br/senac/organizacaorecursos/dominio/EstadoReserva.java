package br.senac.organizacaorecursos.dominio;

import java.util.EnumSet;
import java.util.Set;

/** Estados da reserva (especificação): SOLICITADA → APROVADA → EM_USO → CONCLUIDA, e os alternativos. */
public enum EstadoReserva {
    SOLICITADA,
    APROVADA,
    EM_USO,
    CONCLUIDA,
    REJEITADA,
    CANCELADA,
    NAO_COMPARECEU;

    /** RN-07: transições permitidas (PRD, seção 8.1). Estados finais não saem mais. */
    public boolean podeTransitarPara(EstadoReserva novo) {
        return destinosPermitidos().contains(novo);
    }

    private Set<EstadoReserva> destinosPermitidos() {
        return switch (this) {
            case SOLICITADA -> EnumSet.of(APROVADA, REJEITADA, CANCELADA);
            case APROVADA -> EnumSet.of(CANCELADA, EM_USO, NAO_COMPARECEU);
            case EM_USO -> EnumSet.of(CONCLUIDA);
            case CONCLUIDA, REJEITADA, CANCELADA, NAO_COMPARECEU -> EnumSet.noneOf(EstadoReserva.class);
        };
    }

    /** Estados que bloqueiam o recurso na regra de sobreposição (RN-02). */
    public boolean ocupaRecurso() {
        return this == SOLICITADA || this == APROVADA || this == EM_USO;
    }

    /** RN-08 (D-11): a reserva é iniciada pelo estado, não pelo relógio. */
    public boolean iniciada() {
        return this == EM_USO || this == CONCLUIDA || this == NAO_COMPARECEU;
    }
}
