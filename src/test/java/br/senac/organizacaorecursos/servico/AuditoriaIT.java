package br.senac.organizacaorecursos.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.organizacaorecursos.TestcontainersConfiguration;
import br.senac.organizacaorecursos.dominio.AuditoriaReserva;
import br.senac.organizacaorecursos.dominio.EstadoReserva;
import br.senac.organizacaorecursos.dominio.Intervalo;
import br.senac.organizacaorecursos.dominio.Perfil;
import br.senac.organizacaorecursos.dominio.Recurso;
import br.senac.organizacaorecursos.dominio.Reserva;
import br.senac.organizacaorecursos.dominio.TipoRecurso;
import br.senac.organizacaorecursos.dominio.Usuario;
import br.senac.organizacaorecursos.persistencia.AuditoriaReservaRepository;
import br.senac.organizacaorecursos.persistencia.RecursoRepository;
import br.senac.organizacaorecursos.persistencia.ReservaRepository;
import br.senac.organizacaorecursos.persistencia.UsuarioRepository;
import br.senac.organizacaorecursos.seguranca.ServicoDeToken;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * RF-10 / RN-09: toda mudança de estado gera registro com reserva, usuário, instante (Clock), estado anterior,
 * estado novo e motivo, na mesma transação da mudança (Aula 11). Sem @Transactional na classe: o teste de
 * rollback precisa controlar a própria transação.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuditoriaIT {

    private static final Intervalo OITO_AS_DEZ =
            new Intervalo(Instant.parse("2026-11-12T08:00:00Z"), Instant.parse("2026-11-12T10:00:00Z"));

    @Autowired
    private ServicoDeReservas servico;
    @Autowired
    private AuditoriaReservaRepository auditoria;
    @Autowired
    private ReservaRepository reservas;
    @Autowired
    private RecursoRepository recursos;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private TransactionTemplate transacao;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ServicoDeToken tokens;
    @Autowired
    private JdbcTemplate jdbc;

    private Usuario solicitante;
    private Usuario responsavel;
    private Usuario administrador;
    private Recurso sala;
    private Recurso laboratorio;

    @BeforeEach
    void massa() {
        solicitante = usuario(Perfil.SOLICITANTE);
        responsavel = usuario(Perfil.RESPONSAVEL);
        administrador = usuario(Perfil.ADMINISTRADOR);
        sala = recursos.save(new Recurso(TipoRecurso.SALA, "Sala auditoria " + UUID.randomUUID(), false));
        laboratorio = recursos.save(new Recurso(TipoRecurso.SALA, "Lab auditoria " + UUID.randomUUID(), true));
    }

    @AfterEach
    void limpar() {
        jdbc.update("delete from auditoria_reserva where reserva_id in (select id from reserva where recurso_id in (?, ?))",
                sala.getId(), laboratorio.getId());
        jdbc.update("delete from reserva where recurso_id in (?, ?)", sala.getId(), laboratorio.getId());
    }

    @Test
    void rn09_01_criarReservaDeRecursoNaoRestritoRegistraPedidoEAprovacaoAutomatica() {
        Instant antes = Instant.now();
        Reserva reserva = servico.solicitar(sala.getId(), solicitante.getId(), OITO_AS_DEZ, "Aula");
        Instant depois = Instant.now();

        List<AuditoriaReserva> trilha = auditoria.findByReservaIdOrderByIdAsc(reserva.getId());

        assertThat(trilha).extracting(AuditoriaReserva::getEstadoAnterior, AuditoriaReserva::getEstadoNovo)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(null, EstadoReserva.SOLICITADA),
                        org.assertj.core.groups.Tuple.tuple(EstadoReserva.SOLICITADA, EstadoReserva.APROVADA));
        assertThat(trilha).allSatisfy(registro -> {
            assertThat(registro.getUsuarioId()).isEqualTo(solicitante.getId());
            assertThat(registro.getInstante()).isBetween(antes, depois);
            assertThat(registro.getMotivo()).isNotBlank();
        });
    }

    @Test
    void rn09_02_aprovacaoDeRestritoRegistraQuemAprovou() {
        Reserva reserva = servico.solicitar(laboratorio.getId(), solicitante.getId(), OITO_AS_DEZ, "Prática");

        servico.aprovar(reserva.getId(), responsavel.getId());

        AuditoriaReserva ultimo = ultimoRegistro(reserva.getId());
        assertThat(ultimo.getEstadoAnterior()).isEqualTo(EstadoReserva.SOLICITADA);
        assertThat(ultimo.getEstadoNovo()).isEqualTo(EstadoReserva.APROVADA);
        assertThat(ultimo.getUsuarioId()).isEqualTo(responsavel.getId());
    }

    @Test
    void cancelamentoRegistraTransicaoEMotivo() {
        Reserva reserva = servico.solicitar(sala.getId(), solicitante.getId(), OITO_AS_DEZ, "Aula");

        servico.cancelar(reserva.getId(), solicitante.getId());

        AuditoriaReserva ultimo = ultimoRegistro(reserva.getId());
        assertThat(ultimo.getEstadoAnterior()).isEqualTo(EstadoReserva.APROVADA);
        assertThat(ultimo.getEstadoNovo()).isEqualTo(EstadoReserva.CANCELADA);
    }

    @Test
    void rn09_03_rollbackNaoDeixaAuditoriaOrfa() {
        Reserva reserva = servico.solicitar(sala.getId(), solicitante.getId(), OITO_AS_DEZ, "Aula");
        int registrosAntes = auditoria.findByReservaIdOrderByIdAsc(reserva.getId()).size();

        assertThatThrownBy(() -> transacao.executeWithoutResult(status -> {
            servico.cancelar(reserva.getId(), solicitante.getId());
            throw new IllegalStateException("falha simulada depois da mudança, antes do commit");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(reservas.findById(reserva.getId())).get()
                .extracting(Reserva::getEstado).isEqualTo(EstadoReserva.APROVADA);
        assertThat(auditoria.findByReservaIdOrderByIdAsc(reserva.getId())).hasSize(registrosAntes);
    }

    @Test
    void rn09_06_responsavelConsultaOHistoricoPelaApi() throws Exception {
        Reserva reserva = servico.solicitar(sala.getId(), solicitante.getId(), OITO_AS_DEZ, "Aula");
        servico.cancelar(reserva.getId(), solicitante.getId());

        mvc.perform(get("/api/reservas/" + reserva.getId() + "/historico").header("Authorization", token(responsavel)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[2].estadoNovo").value("CANCELADA"))
                .andExpect(jsonPath("$[2].usuarioNome").value(solicitante.getNome()));
    }

    @Test
    void rn09_04_historicoNaoPodeSerApagadoPelaApi() throws Exception {
        Reserva reserva = servico.solicitar(sala.getId(), solicitante.getId(), OITO_AS_DEZ, "Aula");

        mvc.perform(delete("/api/reservas/" + reserva.getId() + "/historico").header("Authorization", token(administrador)))
                .andExpect(status().isMethodNotAllowed());
        assertThat(auditoria.findByReservaIdOrderByIdAsc(reserva.getId())).isNotEmpty();
    }

    private AuditoriaReserva ultimoRegistro(Long reservaId) {
        List<AuditoriaReserva> trilha = auditoria.findByReservaIdOrderByIdAsc(reservaId);
        assertThat(trilha).isNotEmpty();
        return trilha.get(trilha.size() - 1);
    }

    private Usuario usuario(Perfil perfil) {
        return usuarios.save(new Usuario(perfil.name() + " auditoria",
                perfil.name().toLowerCase() + "-" + UUID.randomUUID() + "@teste.invalid", perfil, "hash-ficticio"));
    }

    private String token(Usuario usuario) {
        return "Bearer " + tokens.emitir(usuario).valor();
    }
}
