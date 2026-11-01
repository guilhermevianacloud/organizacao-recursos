package br.senac.organizacaorecursos.dominio;

/** A reserva pedida não existe. */
public class ReservaNaoEncontradaException extends RegraDeNegocioException {

    public ReservaNaoEncontradaException(Long id) {
        super("RESERVA_NAO_ENCONTRADA", "Reserva " + id + " não encontrada.");
    }
}
