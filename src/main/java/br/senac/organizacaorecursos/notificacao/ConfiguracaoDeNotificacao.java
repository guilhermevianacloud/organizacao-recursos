package br.senac.organizacaorecursos.notificacao;

import br.senac.organizacaorecursos.notificacao.Notificador.Notificacao;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * RF-11 / RN-13: escolhe o adaptador (HTTP se houver URL; simulado se não) e envia a notificação só depois
 * do commit. Transação desfeita não notifica; falha do provedor não volta para a reserva.
 */
@Configuration
public class ConfiguracaoDeNotificacao {

    private static final Logger LOG = LoggerFactory.getLogger(ConfiguracaoDeNotificacao.class);

    @Bean
    Notificador notificador(@Value("${app.notificacao.url:}") String url,
                            @Value("${app.notificacao.chave:}") String chave,
                            @Value("${app.notificacao.timeout:PT2S}") Duration timeout) {
        if (url == null || url.isBlank()) {
            return new NotificadorSimulado();
        }
        return new NotificadorHttp(url, chave, timeout);
    }

    @Bean
    OuvinteDeReservas ouvinteDeReservas(Notificador notificador) {
        return new OuvinteDeReservas(notificador);
    }

    public static class OuvinteDeReservas {

        private final Notificador notificador;

        OuvinteDeReservas(Notificador notificador) {
            this.notificador = notificador;
        }

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void aoMudar(ReservaMudouDeEstado evento) {
            try {
                notificador.notificar(new Notificacao(evento.reservaId(), evento.recurso(), evento.estado(),
                        evento.solicitanteId()));
            } catch (RuntimeException e) {
                LOG.warn("Notificação não enviada: reserva={} motivo={}", evento.reservaId(), e.getClass().getSimpleName());
            }
        }
    }
}
