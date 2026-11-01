package br.senac.organizacaorecursos.notificacao;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

import br.senac.organizacaorecursos.notificacao.Notificador.Notificacao;
import br.senac.organizacaorecursos.notificacao.Notificador.Resultado;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * RF-11 / RN-13: matriz de falhas da integração (Aula 08) contra o provedor simulado pelo WireMock,
 * em porta dinâmica, sem rede externa e sem credencial real.
 */
@WireMockTest(httpPort = 0)
@ExtendWith(OutputCaptureExtension.class)
class NotificadorHttpIT {

    private static final String CHAVE = "chave-ficticia-de-teste-123";
    private static final Notificacao AVISO = new Notificacao(42L, "Sala 101", "APROVADA", 7L);

    private NotificadorHttp notificador;

    @BeforeEach
    void criar(WireMockRuntimeInfo wiremock) {
        notificador = new NotificadorHttp(wiremock.getHttpBaseUrl(), CHAVE, Duration.ofSeconds(2));
    }

    @Test
    void int01_respostaValidaEhEnviadaComOContratoCerto() {
        stubFor(post("/notificacoes").willReturn(okJson("{\"id\":\"n-1\"}")));

        assertThat(notificador.notificar(AVISO)).isEqualTo(Resultado.ENVIADA);
        verify(postRequestedFor(urlEqualTo("/notificacoes"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(matchingJsonPath("$.reservaId", equalTo("42")))
                .withRequestBody(matchingJsonPath("$.estado", equalTo("APROVADA"))));
    }

    @Test
    void int01b_respostaSemIdentificadorNaoContaComoEnviada() {
        stubFor(post("/notificacoes").willReturn(okJson("{}")));

        assertThat(notificador.notificar(AVISO)).isEqualTo(Resultado.FALHOU);
    }

    @Test
    void int02_naoEncontradoViraFalhaControlada() {
        stubFor(post("/notificacoes").willReturn(aResponse().withStatus(404)));

        assertThat(notificador.notificar(AVISO)).isEqualTo(Resultado.FALHOU);
    }

    @Test
    void int03_erroDoProvedorViraFalhaControlada() {
        stubFor(post("/notificacoes").willReturn(aResponse().withStatus(500).withBody("pane interna do provedor")));

        assertThat(notificador.notificar(AVISO)).isEqualTo(Resultado.FALHOU);
    }

    @Test
    void int04_respostaLentaEhInterrompidaNoTimeout() {
        stubFor(post("/notificacoes").willReturn(okJson("{\"id\":\"n-1\"}").withFixedDelay(5_000)));

        long inicio = System.nanoTime();
        Resultado resultado = notificador.notificar(AVISO);
        Duration gasto = Duration.ofNanos(System.nanoTime() - inicio);

        assertThat(resultado).isEqualTo(Resultado.FALHOU);
        assertThat(gasto).isLessThan(Duration.ofMillis(3_500));
    }

    @Test
    void int05_jsonMalformadoViraFalhaControlada() {
        stubFor(post("/notificacoes").willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json").withBody("{\"id\": ")));

        assertThat(notificador.notificar(AVISO)).isEqualTo(Resultado.FALHOU);
    }

    @Test
    void int06_chaveVaiNoCabecalhoMasNuncaAparecemNoLog(CapturedOutput saida) {
        stubFor(post("/notificacoes").willReturn(aResponse().withStatus(401)
                .withBody("chave recusada: " + CHAVE)));

        assertThat(notificador.notificar(AVISO)).isEqualTo(Resultado.FALHOU);

        verify(postRequestedFor(urlEqualTo("/notificacoes")).withHeader("X-Api-Key", equalTo(CHAVE)));
        assertThat(saida.getAll()).contains("reserva=42").doesNotContain(CHAVE);
    }
}
