package br.senac.organizacaorecursos.notificacao;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Adaptador HTTP para o provedor de notificação (ADR-005). Só ele conhece URL, chave e formato externo.
 * Qualquer falha (status de erro, timeout, resposta ilegível) vira {@link Resultado#FALHOU}, sem exceção.
 * O log registra a reserva, o status e o tempo — nunca a chave nem o corpo da resposta (RNF-14, INT-06).
 */
public class NotificadorHttp implements Notificador {

    private static final Logger LOG = LoggerFactory.getLogger(NotificadorHttp.class);

    private final RestClient cliente;

    public NotificadorHttp(String url, String chave, Duration timeout) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(timeout);
        fabrica.setReadTimeout(timeout);
        this.cliente = RestClient.builder()
                .baseUrl(url)
                .requestFactory(fabrica)
                .defaultHeader("X-Api-Key", chave)
                .build();
    }

    @Override
    public Resultado notificar(Notificacao notificacao) {
        long inicio = System.nanoTime();
        try {
            RespostaDoProvedor resposta = cliente.post()
                    .uri("/notificacoes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo(notificacao))
                    .retrieve()
                    .body(RespostaDoProvedor.class);
            if (resposta == null || resposta.id() == null) {
                LOG.warn("Notificação sem confirmação do provedor: reserva={}", notificacao.reservaId());
                return Resultado.FALHOU;
            }
            LOG.info("Notificação enviada: reserva={} duracaoMs={}", notificacao.reservaId(), decorrido(inicio));
            return Resultado.ENVIADA;
        } catch (RestClientResponseException e) {
            LOG.warn("Falha ao notificar: reserva={} status={} duracaoMs={}", notificacao.reservaId(),
                    e.getStatusCode().value(), decorrido(inicio));
            return Resultado.FALHOU;
        } catch (RestClientException e) {
            LOG.warn("Falha ao notificar: reserva={} motivo={} duracaoMs={}", notificacao.reservaId(),
                    e.getClass().getSimpleName(), decorrido(inicio));
            return Resultado.FALHOU;
        }
    }

    private static Map<String, Object> corpo(Notificacao n) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("reservaId", n.reservaId());
        corpo.put("recurso", n.recurso());
        corpo.put("estado", n.estado());
        corpo.put("solicitanteId", n.solicitanteId());
        return corpo;
    }

    private static long decorrido(long inicio) {
        return (System.nanoTime() - inicio) / 1_000_000;
    }

    record RespostaDoProvedor(String id) {
    }
}
