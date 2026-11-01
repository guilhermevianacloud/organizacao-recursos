package br.senac.organizacaorecursos.notificacao;

/** Porta de saída para avisar sobre mudanças de reserva (RF-11, ADR-005). O domínio só conhece esta interface. */
public interface Notificador {

    Resultado notificar(Notificacao notificacao);

    record Notificacao(Long reservaId, String recurso, String estado, Long solicitanteId) {
    }

    enum Resultado {
        ENVIADA,
        FALHOU
    }
}
