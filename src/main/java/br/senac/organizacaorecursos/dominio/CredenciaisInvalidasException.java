package br.senac.organizacaorecursos.dominio;

/** Login recusado. A mensagem é a mesma para e-mail inexistente e senha errada (não revela quem existe). */
public class CredenciaisInvalidasException extends RegraDeNegocioException {

    public CredenciaisInvalidasException() {
        super("CREDENCIAIS_INVALIDAS", "E-mail ou senha incorretos.");
    }
}
