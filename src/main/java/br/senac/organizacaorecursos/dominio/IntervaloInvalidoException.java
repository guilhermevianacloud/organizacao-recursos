package br.senac.organizacaorecursos.dominio;

/** RN-01: o término deve ser posterior ao início. */
public class IntervaloInvalidoException extends RegraDeNegocioException {

    public IntervaloInvalidoException(String mensagem) {
        super("INTERVALO_INVALIDO", mensagem);
    }
}
