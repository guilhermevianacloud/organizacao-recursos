package br.senac.organizacaorecursos.dominio;

/** Q-02 (comportamento provisório): não se cadastra manutenção sobre reserva ativa. */
public class ManutencaoConflitaComReservaException extends RegraDeNegocioException {

    public ManutencaoConflitaComReservaException() {
        super("MANUTENCAO_CONFLITA_COM_RESERVA",
                "Existe reserva ativa nesse período. Cancele ou remaneje antes de bloquear o recurso.");
    }
}
