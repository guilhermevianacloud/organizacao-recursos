package br.senac.organizacaorecursos.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.organizacaorecursos.TestcontainersConfiguration;
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
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** RF-03: pesquisa por tipo, capacidade, localização, competência e disponibilidade. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class PesquisaDeRecursosIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private RecursoRepository recursos;
    @Autowired
    private ReservaRepository reservas;
    @Autowired
    private ManutencaoRepository manutencoes;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private ServicoDeToken tokens;

    private String token;
    private String bloco;
    private String competencia;
    private Recurso sala30;
    private Recurso sala60;

    @BeforeEach
    void massa() {
        Usuario usuario = usuarios.save(new Usuario("Marina", "sol-" + UUID.randomUUID() + "@teste.invalid",
                Perfil.SOLICITANTE, "hash-ficticio"));
        token = "Bearer " + tokens.emitir(usuario).valor();
        bloco = "Bloco " + UUID.randomUUID().toString().substring(0, 8);
        competencia = "Redes " + UUID.randomUUID().toString().substring(0, 8);
        sala30 = recurso(TipoRecurso.SALA, "Sala 30", 30, bloco, null);
        sala60 = recurso(TipoRecurso.SALA, "Sala 60", 60, bloco, null);
        recurso(TipoRecurso.MATERIAL, "Projetor", null, bloco, null);
        recurso(TipoRecurso.PROFESSOR, "Prof. Teste", null, null, competencia);
        reservas.save(Reserva.solicitar(sala60, usuario.getId(), intervalo("08:00", "10:00")));
    }

    @Test
    void filtraPorTipoELocalizacao() throws Exception {
        mvc.perform(pesquisa().param("tipo", "SALA").param("localizacao", bloco.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @ParameterizedTest(name = "capacidade mínima {0} → {1} sala(s)")
    @CsvSource({"29, 2", "30, 2", "31, 1", "60, 1", "61, 0"})
    void filtraPorCapacidadeMinimaNasFronteiras(int minima, int esperadas) throws Exception {
        mvc.perform(pesquisa().param("localizacao", bloco).param("capacidadeMinima", String.valueOf(minima)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(esperadas));
    }

    @Test
    void filtraPorCompetenciaDoProfessor() throws Exception {
        mvc.perform(pesquisa().param("competencia", competencia.toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tipo").value("PROFESSOR"));
    }

    @Test
    void disponibilidadeExcluiRecursoReservadoNoIntervalo() throws Exception {
        mvc.perform(pesquisa().param("tipo", "SALA").param("localizacao", bloco)
                        .param("disponivelInicio", "2026-11-13T09:00:00Z").param("disponivelFim", "2026-11-13T09:30:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(sala30.getId()));
    }

    @Test
    void disponibilidadeAceitaHorarioEncostadoNaReserva() throws Exception {
        mvc.perform(pesquisa().param("tipo", "SALA").param("localizacao", bloco)
                        .param("disponivelInicio", "2026-11-13T10:00:00Z").param("disponivelFim", "2026-11-13T11:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void disponibilidadeExcluiRecursoEmManutencao() throws Exception {
        manutencoes.save(new Manutencao(sala30, intervalo("09:00", "12:00"), "Pintura"));

        mvc.perform(pesquisa().param("tipo", "SALA").param("localizacao", bloco)
                        .param("disponivelInicio", "2026-11-13T09:00:00Z").param("disponivelFim", "2026-11-13T09:30:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void disponibilidadeSoComInicioResponde400() throws Exception {
        mvc.perform(pesquisa().param("disponivelInicio", "2026-11-13T09:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PESQUISA_INVALIDA"));
    }

    private MockHttpServletRequestBuilder pesquisa() {
        return get("/api/recursos").header("Authorization", token);
    }

    private Recurso recurso(TipoRecurso tipo, String nome, Integer capacidade, String localizacao, String competencias) {
        Recurso recurso = new Recurso(tipo, nome, false);
        recurso.setCapacidade(capacidade);
        recurso.setLocalizacao(localizacao);
        recurso.setCompetencias(competencias);
        return recursos.save(recurso);
    }

    private static Intervalo intervalo(String inicio, String termino) {
        return new Intervalo(Instant.parse("2026-11-13T" + inicio + ":00Z"), Instant.parse("2026-11-13T" + termino + ":00Z"));
    }
}
