package br.senac.organizacaorecursos.dominio;

/** RN-10 e RN-06: o usuário não é dono da reserva ou não pode aprovar a própria. */
public class AcessoNegadoException extends RegraDeNegocioException {

    public AcessoNegadoException(String mensagem) {
        super("ACESSO_NEGADO", mensagem);
    }
}
