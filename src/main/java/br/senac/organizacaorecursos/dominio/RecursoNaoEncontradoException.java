package br.senac.organizacaorecursos.dominio;

/** O recurso pedido não existe. */
public class RecursoNaoEncontradoException extends RegraDeNegocioException {

    public RecursoNaoEncontradoException(Long id) {
        super("RECURSO_NAO_ENCONTRADO", "Recurso " + id + " não encontrado.");
    }
}
