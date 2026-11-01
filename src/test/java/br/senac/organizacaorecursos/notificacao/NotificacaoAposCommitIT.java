package br.senac.organizacaorecursos.notificacao;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.senac.organizacaorecursos.TestcontainersConfiguration;
import br.senac.organizacaorecursos.dominio.EstadoReserva;
import br.senac.organizacaorecursos.dominio.Intervalo;
import br.senac.organizacaorecursos.dominio.Perfil;
import br.senac.organizacaorecursos.dominio.Recurso;
import br.senac.organizacaorecursos.dominio.Reserva;
import br.senac.organizacaorecursos.dominio.TipoRecurso;
import br.senac.organizacaorecursos.dominio.Usuario;
import br.senac.organizacaorecursos.persistencia.RecursoRepository;
import br.senac.organizacaorecursos.persistencia.ReservaRepository;
import br.senac.organizacaorecursos.persistencia.UsuarioRepository;
import br.senac.organizacaorecursos.servico.ServicoDeReservas;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * RN-13 com a aplicação inteira: a notificação sai depois do commit; falha do provedor não desfaz a reserva;
 * transação desfeita não notifica. Sem @Transactional: o commit precisa acontecer de verdade.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class NotificacaoAposCommitIT {

    private static final WireMockServer PROVEDOR = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        PROVEDOR.start();
    }

    @DynamicPropertySource
    static void apontarParaOProvedorSimulado(DynamicPropertyRegistry registro) {
        registro.add("app.notificacao.url", PROVEDOR::baseUrl);
        registro.add("app.notificacao.chave", () -> "chave-ficticia");
    }

    @Autowired
    private ServicoDeReservas servico;
    @Autowired
    private ReservaRepository reservas;
    @Autowired
    private RecursoRepository recursos;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private TransactionTemplate transacao;
    @Autowired
    private JdbcTemplate jdbc;

    private Usuario solicitante;
    private Recurso sala;

    @BeforeEach
    void massa() {
        PROVEDOR.resetAll();
        solicitante = usuarios.save(new Usuario("Marina", "sol-" + UUID.randomUUID() + "@teste.invalid",
                Perfil.SOLICITANTE, "hash-ficticio"));
        sala = recursos.save(new Recurso(TipoRecurso.SALA, "Sala notificação " + UUID.randomUUID(), false));
    }

    @AfterEach
    void limpar() {
        jdbc.update("delete from auditoria_reserva where reserva_id in (select id from reserva where recurso_id = ?)", sala.getId());
        jdbc.update("delete from reserva where recurso_id = ?", sala.getId());
    }

    @AfterAll
    static void desligar() {
        PROVEDOR.stop();
    }

    @Test
    void rn13_03_provedorComErroNaoDesfazAReserva() {
        PROVEDOR.stubFor(post("/notificacoes").willReturn(aResponse().withStatus(500)));

        Reserva reserva = servico.solicitar(sala.getId(), solicitante.getId(), intervalo(), "Aula");

        assertThat(reservas.findById(reserva.getId())).get()
                .extracting(Reserva::getEstado).isEqualTo(EstadoReserva.APROVADA);
        PROVEDOR.verify(postRequestedFor(urlEqualTo("/notificacoes"))
                .withRequestBody(matchingJsonPath("$.reservaId", com.github.tomakehurst.wiremock.client.WireMock
                        .equalTo(reserva.getId().toString()))));
    }

    @Test
    void transacaoDesfeitaNaoNotifica() {
        PROVEDOR.stubFor(post("/notificacoes").willReturn(aResponse().withStatus(200).withBody("{\"id\":\"x\"}")));

        assertThatThrownBy(() -> transacao.executeWithoutResult(status -> {
            servico.solicitar(sala.getId(), solicitante.getId(), intervalo(), "Aula");
            throw new IllegalStateException("falha simulada antes do commit");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(PROVEDOR.findAll(postRequestedFor(urlEqualTo("/notificacoes")))).isEmpty();
    }

    private static Intervalo intervalo() {
        return new Intervalo(Instant.parse("2026-11-16T08:00:00Z"), Instant.parse("2026-11-16T10:00:00Z"));
    }
}
