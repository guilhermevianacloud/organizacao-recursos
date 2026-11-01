package br.senac.organizacaorecursos.dominio;

/** RN-04: o recurso está em manutenção no intervalo pedido. */
public class RecursoEmManutencaoException extends RegraDeNegocioException {

    public RecursoEmManutencaoException() {
        super("RECURSO_EM_MANUTENCAO", "O recurso está em manutenção nesse horário.");
    }
}
