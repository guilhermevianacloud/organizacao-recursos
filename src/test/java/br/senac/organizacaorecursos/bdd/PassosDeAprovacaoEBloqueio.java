package br.senac.organizacaorecursos.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import br.senac.organizacaorecursos.TestcontainersConfiguration;
import br.senac.organizacaorecursos.dominio.EstadoReserva;
import br.senac.organizacaorecursos.dominio.Intervalo;
import br.senac.organizacaorecursos.dominio.Manutencao;
import br.senac.organizacaorecursos.dominio.Perfil;
import br.senac.organizacaorecursos.dominio.Recurso;
import br.senac.organizacaorecursos.dominio.Reserva;
import br.senac.organizacaorecursos.dominio.TipoRecurso;
import br.senac.organizacaorecursos.dominio.Usuario;
import br.senac.organizacaorecursos.persistencia.ManutencaoRepository;
import br.senac.organizacaorecursos.persistencia.RecursoRepository;
import br.senac.organizacaorecursos.persistencia.ReservaRepository;
import br.senac.organizacaorecursos.persistencia.UsuarioRepository;
import br.senac.organizacaorecursos.seguranca.ServicoDeToken;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import io.cucumber.spring.CucumberContextConfiguration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Passos dos cenários de aprovacao-bloqueio.feature. Cada passo chama a API com MockMvc e o token real do perfil,
 * usando as mesmas palavras do cenário. Cada cenário cria usuários e recursos próprios.
 */
@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public class PassosDeAprovacaoEBloqueio {

    private static final String DIA = "2032-01-15T";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private RecursoRepository recursos;
    @Autowired
    private ReservaRepository reservas;
    @Autowired
    private ManutencaoRepository manutencoes;
    @Autowired
    private ServicoDeToken tokens;

    private Usuario solicitante;
    private Usuario responsavel;
    private Recurso recurso;
    private Long reservaId;
    private MvcResult resposta;

    @Before
    public void novosUsuarios() {
        solicitante = usuario(Perfil.SOLICITANTE);
        responsavel = usuario(Perfil.RESPONSAVEL);
    }

    @Dado("um recurso restrito")
    public void umRecursoRestrito() {
        recurso = recursos.save(new Recurso(TipoRecurso.SALA, "Laboratório BDD " + UUID.randomUUID(), true));
    }

    @Dado("uma reserva de recurso restrito aguardando aprovação")
    public void umaReservaAguardandoAprovacao() {
        umRecursoRestrito();
        reservaId = reservas.saveAndFlush(Reserva.solicitar(recurso, solicitante.getId(), intervalo("08:00:00", "10:00:00")))
                .getId();
    }

    @Dado("uma sala em manutenção das {word} às {word}")
    public void umaSalaEmManutencao(String inicio, String fim) {
        recurso = recursos.save(new Recurso(TipoRecurso.SALA, "Sala BDD " + UUID.randomUUID(), false));
        manutencoes.save(new Manutencao(recurso, intervalo(inicio, fim), "Manutenção do cenário"));
    }

    @Dado("uma reserva no estado {string}")
    public void umaReservaNoEstado(String estado) {
        boolean restrito = estado.equals("SOLICITADA") || estado.equals("REJEITADA");
        recurso = recursos.save(new Recurso(TipoRecurso.SALA, "Sala estados " + UUID.randomUUID(), restrito));
        Reserva reserva = Reserva.solicitar(recurso, solicitante.getId(), intervalo("08:00:00", "10:00:00"));
        switch (EstadoReserva.valueOf(estado)) {
            case CANCELADA -> reserva.transitarPara(EstadoReserva.CANCELADA);
            case REJEITADA -> reserva.transitarPara(EstadoReserva.REJEITADA);
            case NAO_COMPARECEU -> reserva.transitarPara(EstadoReserva.NAO_COMPARECEU);
            case EM_USO -> reserva.transitarPara(EstadoReserva.EM_USO);
            case CONCLUIDA -> {
                reserva.transitarPara(EstadoReserva.EM_USO);
                reserva.transitarPara(EstadoReserva.CONCLUIDA);
            }
            default -> {
            }
        }
        reservaId = reservas.saveAndFlush(reserva).getId();
        assertThat(reservas.findById(reservaId)).get().extracting(Reserva::getEstado)
                .isEqualTo(EstadoReserva.valueOf(estado));
    }

    @Quando("o Solicitante pede o recurso das {word} às {word}")
    @Quando("o Solicitante pede a sala das {word} às {word}")
    public void oSolicitantePede(String inicio, String fim) throws Exception {
        resposta = mvc.perform(post("/api/reservas").header("Authorization", token(solicitante))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recursoId\":" + recurso.getId() + ",\"inicio\":\"" + DIA + inicio + "Z\",\"termino\":\""
                        + DIA + fim + "Z\"}")).andReturn();
        if (resposta.getResponse().getStatus() == 201) {
            reservaId = ((Number) JsonPath.read(resposta.getResponse().getContentAsString(), "$.id")).longValue();
        }
    }

    @Quando("o Responsável aprova a reserva")
    public void oResponsavelAprova() throws Exception {
        resposta = mvc.perform(post("/api/reservas/" + reservaId + "/aprovacao").header("Authorization", token(responsavel)))
                .andReturn();
    }

    @Quando("o próprio Solicitante tenta aprovar a reserva")
    public void oProprioSolicitanteTentaAprovar() throws Exception {
        resposta = mvc.perform(post("/api/reservas/" + reservaId + "/aprovacao").header("Authorization", token(solicitante)))
                .andReturn();
    }

    @Quando("o Responsável registra {string}")
    public void oResponsavelRegistra(String acao) throws Exception {
        resposta = mvc.perform(post("/api/reservas/" + reservaId + "/" + acao).header("Authorization", token(responsavel)))
                .andReturn();
    }

    @Então("a resposta é {int}")
    public void aRespostaE(int status) throws Exception {
        assertThat(resposta.getResponse().getStatus())
                .as("corpo: %s", resposta.getResponse().getContentAsString())
                .isEqualTo(status);
    }

    @E("a reserva fica no estado {string}")
    public void aReservaFicaNoEstado(String estado) {
        assertThat(reservas.findById(reservaId)).get().extracting(Reserva::getEstado)
                .isEqualTo(EstadoReserva.valueOf(estado));
    }

    private static Intervalo intervalo(String inicio, String fim) {
        return new Intervalo(Instant.parse(DIA + inicio + "Z"), Instant.parse(DIA + fim + "Z"));
    }

    private Usuario usuario(Perfil perfil) {
        return usuarios.save(new Usuario(perfil.name() + " BDD", perfil.name().toLowerCase() + "-" + UUID.randomUUID()
                + "@teste.invalid", perfil, "hash-ficticio"));
    }

    private String token(Usuario usuario) {
        return "Bearer " + tokens.emitir(usuario).valor();
    }
}
