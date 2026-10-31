package br.senac.organizacaorecursos.dominio;

/** RN-02 e RN-03: o recurso já está ocupado no intervalo pedido. */
public class ConflitoDeHorarioException extends RegraDeNegocioException {

    public ConflitoDeHorarioException() {
        super("CONFLITO_DE_HORARIO", "O recurso já está reservado nesse horário.");
    }
}
