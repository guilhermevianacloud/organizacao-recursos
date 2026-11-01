package br.senac.organizacaorecursos.notificacao;

import java.time.Duration;

/** Adaptador HTTP para o provedor de notificação. Só ele conhece URL, chave e formato externo. */
public class NotificadorHttp implements Notificador {

    public NotificadorHttp(String url, String chave, Duration timeout) {
    }

    @Override
    public Resultado notificar(Notificacao notificacao) {
        return Resultado.ENVIADA;
    }
}
