package br.senac.organizacaorecursos.notificacao;

/** Publicado pelo serviço de reservas a cada mudança; vira notificação só depois do commit. */
public record ReservaMudouDeEstado(Long reservaId, String recurso, String estado, Long solicitanteId) {
}
