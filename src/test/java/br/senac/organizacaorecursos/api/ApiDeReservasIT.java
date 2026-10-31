package br.senac.organizacaorecursos.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.organizacaorecursos.TestcontainersConfiguration;
import br.senac.organizacaorecursos.dominio.Perfil;
import br.senac.organizacaorecursos.dominio.Recurso;
import br.senac.organizacaorecursos.dominio.TipoRecurso;
import br.senac.organizacaorecursos.dominio.Usuario;
import br.senac.organizacaorecursos.persistencia.RecursoRepository;
import br.senac.organizacaorecursos.persistencia.UsuarioRepository;
import br.senac.organizacaorecursos.seguranca.ServicoDeToken;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** Contrato HTTP em caixa-preta: RN-12 (400 compreensível), RN-02 (409), 404, login e erro neutro (RNF-06). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ApiDeReservasIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private RecursoRepository recursos;
    @Autowired
    private ServicoDeToken tokens;
    @Autowired
    private PasswordEncoder codificador;

    private Usuario solicitante;
    private String tokenSolicitante;
    private String tokenAdministrador;
    private Long sala;

    @BeforeEach
    void massa() {
        solicitante = usuarios.save(new Usuario("Marina", "sol-" + UUID.randomUUID() + "@teste.invalid",
                Perfil.SOLICITANTE, codificador.encode("senha-certa")));
        Usuario admin = usuarios.save(new Usuario("Cláudia", "adm-" + UUID.randomUUID() + "@teste.invalid",
                Perfil.ADMINISTRADOR, codificador.encode("senha-certa")));
        tokenSolicitante = "Bearer " + tokens.emitir(solicitante).valor();
        tokenAdministrador = "Bearer " + tokens.emitir(admin).valor();
        sala = recursos.save(new Recurso(TipoRecurso.SALA, "Sala API", false)).getId();
    }

    private ResultActions solicitar(String inicio, String termino) throws Exception {
        return mvc.perform(post("/api/reservas").header("Authorization", tokenSolicitante)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recursoId\":" + sala + ",\"inicio\":\"2026-11-09T" + inicio + ":00Z\",\"termino\":\"2026-11-09T"
                        + termino + ":00Z\",\"motivo\":\"Aula\"}"));
    }

    @Test
    void criaReservaERespondeComOEstado() throws Exception {
        solicitar("08:00", "10:00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("APROVADA"))
                .andExpect(jsonPath("$.recursoNome").value("Sala API"));
    }

    @Test
    void rn02_conflitoDeHorarioResponde409ComCodigo() throws Exception {
        solicitar("08:00", "10:00").andExpect(status().isCreated());

        String corpo = solicitar("09:00", "11:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_DE_HORARIO"))
                .andReturn().getResponse().getContentAsString();
        semDetalheInterno(corpo);
    }

    @Test
    void rn01_terminoAntesDoInicioResponde400Compreensivel() throws Exception {
        solicitar("10:00", "08:00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("INTERVALO_INVALIDO"))
                .andExpect(jsonPath("$.detail").value("O término deve ser posterior ao início."));
    }

    @Test
    void rn12_01_capacidadeNegativaResponde400ApontandoOCampo() throws Exception {
        String corpo = mvc.perform(post("/api/recursos").header("Authorization", tokenAdministrador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"SALA\",\"nome\":\"Sala X\",\"capacidade\":-5,\"restrito\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"))
                .andReturn().getResponse().getContentAsString();
        assertThat(corpo).contains("capacidade");
        semDetalheInterno(corpo);
    }

    @Test
    void rn12_02_campoObrigatorioAusenteResponde400() throws Exception {
        mvc.perform(post("/api/reservas").header("Authorization", tokenSolicitante)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inicio\":\"2026-11-09T08:00:00Z\",\"termino\":\"2026-11-09T09:00:00Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("recursoId")));
    }

    @Test
    void rn12_03_jsonMalformadoResponde400SemDetalheInterno() throws Exception {
        String corpo = mvc.perform(post("/api/reservas").header("Authorization", tokenSolicitante)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"recursoId\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CORPO_INVALIDO"))
                .andReturn().getResponse().getContentAsString();
        semDetalheInterno(corpo);
    }

    @Test
    void rn10_04_reservaInexistenteResponde404() throws Exception {
        mvc.perform(post("/api/reservas/999999/cancelamento").header("Authorization", tokenSolicitante))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RESERVA_NAO_ENCONTRADA"));
    }

    @Test
    void loginComSenhaErradaResponde401SemDizerSeOEmailExiste() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + solicitante.getEmail() + "\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos."));
    }

    @Test
    void loginDevolveTokenQueAbreAApi() throws Exception {
        String corpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + solicitante.getEmail() + "\",\"senha\":\"senha-certa\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("SOLICITANTE"))
                .andReturn().getResponse().getContentAsString();
        String token = corpo.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        mvc.perform(get("/api/reservas/minhas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void tokenAdulteradoResponde401() throws Exception {
        String adulterado = tokenSolicitante.substring(0, tokenSolicitante.length() - 3) + "abc";

        mvc.perform(get("/api/reservas/minhas").header("Authorization", adulterado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    private static void semDetalheInterno(String corpo) {
        assertThat(corpo).doesNotContain("Exception", "at br.", "org.", "java.", "SQL", "constraint", "select ");
    }
}
