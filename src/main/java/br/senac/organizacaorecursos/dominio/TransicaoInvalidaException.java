package br.senac.organizacaorecursos.dominio;

/** RN-07: a reserva só muda de estado pelas transições permitidas. */
public class TransicaoInvalidaException extends RegraDeNegocioException {

    public TransicaoInvalidaException(EstadoReserva de, EstadoReserva para) {
        super("TRANSICAO_INVALIDA", "Não é possível passar a reserva de " + de + " para " + para + ".");
    }
}
