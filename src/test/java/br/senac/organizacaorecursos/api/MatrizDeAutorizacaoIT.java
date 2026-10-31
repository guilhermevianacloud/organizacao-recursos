package br.senac.organizacaorecursos.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

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
import br.senac.organizacaorecursos.seguranca.ServicoDeToken;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-01 / RN-10 / RNF-04: lê docs/seguranca/matriz-autorizacao.md e executa cada célula
 * (operação × perfil) contra a API com MockMvc e tokens reais (Aula 12).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class MatrizDeAutorizacaoIT {

    private static final Path MATRIZ = Path.of("docs", "seguranca", "matriz-autorizacao.md");
    private static final String[] PERFIS = {"Anônimo", "Solicitante", "Responsável", "Administrador"};
    private static final String SENHA = "senha-teste";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private RecursoRepository recursos;
    @Autowired
    private ReservaRepository reservas;
    @Autowired
    private ServicoDeToken tokens;
    @Autowired
    private PasswordEncoder codificador;

    private final Map<String, Usuario> usuarioDoPerfil = new HashMap<>();
    private final Map<String, String> marcadores = new HashMap<>();
    private Long salaLivre;

    record Celula(String id, String operacao, String metodo, String rota, String perfil, int esperado) {
        @Override
        public String toString() {
            return id + " " + operacao + " como " + perfil + " → " + esperado;
        }
    }

    static Stream<Arguments> celulas() throws IOException {
        List<Arguments> lista = new ArrayList<>();
        for (String linha : Files.readAllLines(MATRIZ, StandardCharsets.UTF_8)) {
            String[] partes = linha.split("\\|");
            if (partes.length < 9 || !partes[1].trim().matches("[A-Z]{3}-\\d{2}")) {
                continue;
            }
            for (int i = 0; i < PERFIS.length; i++) {
                lista.add(Arguments.of(new Celula(partes[1].trim(), partes[2].trim(), partes[3].trim(),
                        partes[4].trim(), PERFIS[i], Integer.parseInt(partes[5 + i].trim()))));
            }
        }
        assertThat(lista).as("células lidas da matriz").isNotEmpty();
        return lista.stream();
    }

    @BeforeEach
    void prepararMassa() {
        String hash = codificador.encode(SENHA);
        Usuario solicitante = novoUsuario("Solicitante", Perfil.SOLICITANTE, hash);
        Usuario outro = novoUsuario("Outro solicitante", Perfil.SOLICITANTE, hash);
        usuarioDoPerfil.put("Solicitante", solicitante);
        usuarioDoPerfil.put("Responsável", novoUsuario("Responsável", Perfil.RESPONSAVEL, hash));
        usuarioDoPerfil.put("Administrador", novoUsuario("Administrador", Perfil.ADMINISTRADOR, hash));

        Recurso sala = recursos.save(new Recurso(TipoRecurso.SALA, "Sala da matriz", false));
        Recurso laboratorio = recursos.save(new Recurso(TipoRecurso.SALA, "Laboratório da matriz", true));
        salaLivre = recursos.save(new Recurso(TipoRecurso.SALA, "Sala livre", false)).getId();

        marcadores.put("{recurso}", sala.getId().toString());
        marcadores.put("{reservaPropria}", reserva(sala, solicitante, "08:00", "10:00").getId().toString());
        marcadores.put("{reservaDeOutro}", reserva(sala, outro, "10:00", "12:00").getId().toString());
        marcadores.put("{reservaRestrita}", reserva(laboratorio, outro, "08:00", "10:00").getId().toString());
        marcadores.put("{reservaAprovada}", reserva(sala, outro, "13:00", "14:00").getId().toString());
        Reserva emUso = reserva(sala, outro, "15:00", "16:00");
        emUso.transitarPara(EstadoReserva.EM_USO);
        marcadores.put("{reservaEmUso}", reservas.saveAndFlush(emUso).getId().toString());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("celulas")
    void cadaCelulaDaMatrizTemOStatusEsperado(Celula celula) throws Exception {
        String rota = celula.rota();
        for (Map.Entry<String, String> m : marcadores.entrySet()) {
            rota = rota.replace(m.getKey(), m.getValue());
        }
        MockHttpServletRequestBuilder pedido = request(HttpMethod.valueOf(celula.metodo()), rota)
                .contentType(MediaType.APPLICATION_JSON);
        String corpo = corpoDe(celula.id());
        if (corpo != null) {
            pedido.content(corpo);
        }
        Usuario usuario = usuarioDoPerfil.get(celula.perfil());
        if (usuario != null) {
            pedido.header("Authorization", "Bearer " + tokens.emitir(usuario).valor());
        }

        MvcResult resultado = mvc.perform(pedido).andReturn();

        assertThat(resultado.getResponse().getStatus())
                .as("%s — corpo: %s", celula, resultado.getResponse().getContentAsString())
                .isEqualTo(celula.esperado());
    }

    private String corpoDe(String id) {
        return switch (id) {
            case "AUT-01" -> "{\"email\":\"" + usuarioDoPerfil.get("Solicitante").getEmail() + "\",\"senha\":\"" + SENHA + "\"}";
            case "REC-03", "REC-04" ->
                    "{\"tipo\":\"SALA\",\"nome\":\"Sala nova\",\"capacidade\":20,\"localizacao\":\"Bloco D\",\"restrito\":false}";
            case "MAN-02" -> "{\"recursoId\":" + salaLivre
                    + ",\"inicio\":\"2026-11-10T13:00:00Z\",\"termino\":\"2026-11-10T14:00:00Z\",\"motivo\":\"Pintura\"}";
            case "RES-03" -> "{\"recursoId\":" + salaLivre
                    + ",\"inicio\":\"2026-11-11T08:00:00Z\",\"termino\":\"2026-11-11T10:00:00Z\",\"motivo\":\"Aula\"}";
            case "RES-04", "RES-07" -> "{\"inicio\":\"2026-11-09T18:00:00Z\",\"termino\":\"2026-11-09T19:00:00Z\"}";
            default -> null;
        };
    }

    private Usuario novoUsuario(String nome, Perfil perfil, String hash) {
        String email = perfil.name().toLowerCase() + "-" + UUID.randomUUID() + "@teste.invalid";
        return usuarios.save(new Usuario(nome, email, perfil, hash));
    }

    private Reserva reserva(Recurso recurso, Usuario dono, String inicio, String termino) {
        Intervalo intervalo = new Intervalo(Instant.parse("2026-11-09T" + inicio + ":00Z"),
                Instant.parse("2026-11-09T" + termino + ":00Z"));
        return reservas.saveAndFlush(Reserva.solicitar(recurso, dono.getId(), intervalo));
    }
}
