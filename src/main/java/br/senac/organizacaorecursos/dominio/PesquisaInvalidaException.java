package br.senac.organizacaorecursos.dominio;

/** RF-03: filtro de disponibilidade incompleto (precisa de início e fim). */
public class PesquisaInvalidaException extends RegraDeNegocioException {

    public PesquisaInvalidaException(String mensagem) {
        super("PESQUISA_INVALIDA", mensagem);
    }
}
