package br.senac.organizacaorecursos.api;

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
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * RF-12 / RN-11, em caixa-preta (Aula 11): massa pequena semeada pela própria API, valores esperados
 * calculados à mão no comentário de cada teste — nunca copiados da saída da aplicação.
 * Período usado só por este teste: 2031-03-10 (UTC). Sem @Transactional: os conflitos evitados são
 * gravados numa transação própria; a limpeza é feita ao final.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RelatorioDeUtilizacaoIT {

    private static final String DIA = "2031-03-10";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private RecursoRepository recursos;
    @Autowired
    private ServicoDeToken tokens;
    @Autowired
    private JdbcTemplate jdbc;

    private String solicitante;
    private String responsavel;
    private Recurso salaA;
    private Recurso salaB;
    private Recurso professor;
    private Recurso laboratorio;

    @BeforeEach
    void massa() {
        solicitante = token(Perfil.SOLICITANTE);
        responsavel = token(Perfil.RESPONSAVEL);
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        salaA = recursos.save(new Recurso(TipoRecurso.SALA, "A Sala " + sufixo, false));
        salaB = recursos.save(new Recurso(TipoRecurso.SALA, "B Sala " + sufixo, false));
        professor = recursos.save(new Recurso(TipoRecurso.PROFESSOR, "C Professor " + sufixo, false));
        laboratorio = recursos.save(new Recurso(TipoRecurso.SALA, "D Lab " + sufixo, true));
    }

    @AfterEach
    void limpar() {
        List<Long> ids = List.of(salaA.getId(), salaB.getId(), professor.getId(), laboratorio.getId());
        for (Long id : ids) {
            jdbc.update("delete from auditoria_reserva where reserva_id in (select id from reserva where recurso_id = ?)", id);
            jdbc.update("delete from reserva where recurso_id = ?", id);
            jdbc.update("delete from tentativa_recusada where recurso_id = ?", id);
        }
    }

    /*
     * Conta feita à mão (período 2031-03-10 00:00 → 2031-03-11 00:00):
     *   Sala A: 08:00–10:00 aprovada = 2,0 h; 13:00–14:30 aprovada = 1,5 h; 15:00–18:00 CANCELADA = não conta
     *           → 2,0 + 1,5 = 3,5 h
     *   Sala B: 22:00 do dia 10 → 02:00 do dia 11 cruza a borda; só 22:00–24:00 está no período (D-14) → 2,0 h
     *   Professor: 09:00–11:00, depois retirada (EM_USO, conta como uso) → 2,0 h; carga do professor = 2,0 h
     *   Laboratório restrito: 08:00–09:00 ainda SOLICITADA (não aprovada) → não conta, não aparece
     *   Conflitos evitados: Sala A 09:00–09:30, Sala A 13:30–15:00, Professor 10:00–12:00 → 3 recusas por sobreposição
     */
    @Test
    void rn11_01_02_03_05_horasPorRecursoCargaDoProfessorEConflitosEvitados() throws Exception {
        criar(salaA, DIA + "T08:00:00Z", DIA + "T10:00:00Z").andExpect(status().isCreated());
        criar(salaA, DIA + "T13:00:00Z", DIA + "T14:30:00Z").andExpect(status().isCreated());
        Long cancelada = id(criar(salaA, DIA + "T15:00:00Z", DIA + "T18:00:00Z"));
        mvc.perform(post("/api/reservas/" + cancelada + "/cancelamento").header("Authorization", solicitante))
                .andExpect(status().isOk());
        criar(salaB, DIA + "T22:00:00Z", "2031-03-11T02:00:00Z").andExpect(status().isCreated());
        Long aulaDoProfessor = id(criar(professor, DIA + "T09:00:00Z", DIA + "T11:00:00Z"));
        mvc.perform(post("/api/reservas/" + aulaDoProfessor + "/retirada").header("Authorization", responsavel))
                .andExpect(status().isOk());
        criar(laboratorio, DIA + "T08:00:00Z", DIA + "T09:00:00Z").andExpect(status().isCreated());

        criar(salaA, DIA + "T09:00:00Z", DIA + "T09:30:00Z").andExpect(status().isConflict());
        criar(salaA, DIA + "T13:30:00Z", DIA + "T15:00:00Z").andExpect(status().isConflict());
        criar(professor, DIA + "T10:00:00Z", DIA + "T12:00:00Z").andExpect(status().isConflict());

        relatorio(DIA + "T00:00:00Z", "2031-03-11T00:00:00Z")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recursos.length()").value(3))
                .andExpect(jsonPath("$.recursos[0].nome").value(salaA.getNome()))
                .andExpect(jsonPath("$.recursos[0].horas").value(3.5))
                .andExpect(jsonPath("$.recursos[1].nome").value(salaB.getNome()))
                .andExpect(jsonPath("$.recursos[1].horas").value(2.0))
                .andExpect(jsonPath("$.recursos[2].nome").value(professor.getNome()))
                .andExpect(jsonPath("$.recursos[2].horas").value(2.0))
                .andExpect(jsonPath("$.professores.length()").value(1))
                .andExpect(jsonPath("$.professores[0].horas").value(2.0))
                .andExpect(jsonPath("$.conflitosEvitados").value(3));
    }

    /*
     * Borda inicial (lacuna apontada pelo JaCoCo): reserva de 2031-05-09 22:00 até 2031-05-10 02:00.
     * Período 2031-05-10 00:00 → 2031-05-11 00:00: só 00:00–02:00 está dentro → 2,0 h (D-14).
     */
    @Test
    void reservaQueComecaAntesDoPeriodoContaSoAsHorasDentro() throws Exception {
        criar(salaA, "2031-05-09T22:00:00Z", "2031-05-10T02:00:00Z").andExpect(status().isCreated());

        relatorio("2031-05-10T00:00:00Z", "2031-05-11T00:00:00Z")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recursos.length()").value(1))
                .andExpect(jsonPath("$.recursos[0].horas").value(2.0));
    }

    /* Período sem nenhuma reserva nem recusa (2031-04-01): listas vazias e 0 conflitos. */
    @Test
    void rn11_04_periodoVazio() throws Exception {
        relatorio("2031-04-01T00:00:00Z", "2031-04-02T00:00:00Z")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recursos.length()").value(0))
                .andExpect(jsonPath("$.professores.length()").value(0))
                .andExpect(jsonPath("$.conflitosEvitados").value(0));
    }

    @Test
    void periodoInvertidoResponde400() throws Exception {
        relatorio("2031-04-02T00:00:00Z", "2031-04-01T00:00:00Z")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("INTERVALO_INVALIDO"));
    }

    private ResultActions criar(Recurso recurso, String inicio, String termino) throws Exception {
        return mvc.perform(post("/api/reservas").header("Authorization", solicitante)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recursoId\":" + recurso.getId() + ",\"inicio\":\"" + inicio + "\",\"termino\":\"" + termino + "\"}"));
    }

    private Long id(ResultActions resultado) throws Exception {
        String corpo = resultado.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }

    private ResultActions relatorio(String inicio, String fim) throws Exception {
        return mvc.perform(get("/api/relatorios/utilizacao").param("inicio", inicio).param("fim", fim)
                .header("Authorization", responsavel));
    }

    private String token(Perfil perfil) {
        Usuario usuario = usuarios.save(new Usuario(perfil.name(), perfil.name().toLowerCase() + "-" + UUID.randomUUID()
                + "@teste.invalid", perfil, "hash-ficticio"));
        return "Bearer " + tokens.emitir(usuario).valor();
    }
}
