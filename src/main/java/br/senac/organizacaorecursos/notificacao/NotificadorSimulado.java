package br.senac.organizacaorecursos.notificacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Usado quando nenhum provedor está configurado (RF-11 aceita notificação simulada): só registra no log. */
public class NotificadorSimulado implements Notificador {

    private static final Logger LOG = LoggerFactory.getLogger(NotificadorSimulado.class);

    @Override
    public Resultado notificar(Notificacao n) {
        LOG.info("Notificação simulada: reserva={} recurso={} estado={}", n.reservaId(), n.recurso(), n.estado());
        return Resultado.ENVIADA;
    }
}
