package br.senac.organizacaorecursos.servico;

import br.senac.organizacaorecursos.dominio.AcessoNegadoException;
import br.senac.organizacaorecursos.dominio.AuditoriaReserva;
import br.senac.organizacaorecursos.persistencia.AuditoriaReservaRepository;
import java.time.Clock;
import br.senac.organizacaorecursos.dominio.ConflitoDeHorarioException;
import br.senac.organizacaorecursos.dominio.EstadoReserva;
import br.senac.organizacaorecursos.dominio.Intervalo;
import br.senac.organizacaorecursos.dominio.Recurso;
import br.senac.organizacaorecursos.dominio.RecursoEmManutencaoException;
import br.senac.organizacaorecursos.dominio.RecursoNaoEncontradoException;
import br.senac.organizacaorecursos.dominio.Reserva;
import br.senac.organizacaorecursos.dominio.ReservaNaoEncontradaException;
import br.senac.organizacaorecursos.persistencia.ManutencaoRepository;
import br.senac.organizacaorecursos.persistencia.RecursoRepository;
import br.senac.organizacaorecursos.persistencia.ReservaRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDeReservas {

    /** Estados que ocupam o recurso (RN-02): só eles entram na verificação de sobreposição. */
    public static final Set<EstadoReserva> ESTADOS_QUE_OCUPAM = Arrays.stream(EstadoReserva.values())
            .filter(EstadoReserva::ocupaRecurso)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(EstadoReserva.class)));

    /** Nome da exclusion constraint criada na migração V3. */
    static final String CONSTRAINT_SEM_SOBREPOSICAO = "reserva_sem_sobreposicao";

    private final RecursoRepository recursos;
    private final ReservaRepository reservas;
    private final ManutencaoRepository manutencoes;
    private final AuditoriaReservaRepository auditoria;
    private final Clock relogio;

    public ServicoDeReservas(RecursoRepository recursos, ReservaRepository reservas,
                             ManutencaoRepository manutencoes, AuditoriaReservaRepository auditoria, Clock relogio) {
        this.recursos = recursos;
        this.reservas = reservas;
        this.manutencoes = manutencoes;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    @Transactional
    public Reserva solicitar(Long recursoId, Long solicitanteId, Intervalo intervalo) {
        return solicitar(recursoId, solicitanteId, intervalo, null);
    }

    /**
     * RN-04 e RN-02: recusa se o recurso estiver em manutenção ou já ocupado no intervalo.
     * RN-03 em duas camadas (ADR-002):
     * 1. a linha do recurso é travada, então pedidos pelo mesmo recurso são atendidos em fila
     *    e a verificação abaixo enxerga a reserva de quem chegou antes (sem deadlock, DEF-002);
     * 2. a constraint reserva_sem_sobreposicao garante a regra para qualquer caminho de escrita.
     */
    @Transactional
    public Reserva solicitar(Long recursoId, Long solicitanteId, Intervalo intervalo, String motivo) {
        Recurso recurso = travarRecurso(recursoId);
        verificarDisponibilidade(recursoId, intervalo, null);
        Reserva reserva = gravar(Reserva.solicitar(recurso, solicitanteId, intervalo, motivo));
        auditar(reserva, solicitanteId, null, EstadoReserva.SOLICITADA, "Pedido de reserva");
        if (reserva.getEstado() == EstadoReserva.APROVADA) {
            auditar(reserva, solicitanteId, EstadoReserva.SOLICITADA, EstadoReserva.APROVADA,
                    "Aprovação automática: recurso não restrito");
        }
        return reserva;
    }

    /** Muda o horário de uma reserva própria (RN-10), com as mesmas verificações da criação. */
    @Transactional
    public Reserva alterar(Long reservaId, Long usuarioId, Intervalo novo) {
        Reserva reserva = obter(reservaId);
        exigirDono(reserva, usuarioId);
        travarRecurso(reserva.getRecurso().getId());
        verificarDisponibilidade(reserva.getRecurso().getId(), novo, reservaId);
        Intervalo anterior = reserva.intervalo();
        reserva.alterarHorario(novo);
        Reserva alterada = gravar(reserva);
        auditar(alterada, usuarioId, alterada.getEstado(), alterada.getEstado(),
                "Horário alterado de " + anterior.inicio() + "–" + anterior.termino()
                        + " para " + novo.inicio() + "–" + novo.termino());
        return alterada;
    }

    /** Cancelar muda o estado (RN-07); nunca apaga a reserva (RN-08). Só o dono cancela (RN-10). */
    @Transactional
    public Reserva cancelar(Long reservaId, Long usuarioId) {
        Reserva reserva = obter(reservaId);
        exigirDono(reserva, usuarioId);
        return transitar(reserva, EstadoReserva.CANCELADA, usuarioId, "Cancelada pelo solicitante");
    }

    /** RN-05 (perfil verificado na API) e RN-06: quem pediu não aprova a própria reserva. */
    @Transactional
    public Reserva aprovar(Long reservaId, Long responsavelId) {
        Reserva reserva = obter(reservaId);
        if (reserva.pertenceA(responsavelId)) {
            throw new AcessoNegadoException("Quem solicitou a reserva não pode aprová-la.");
        }
        return transitar(reserva, EstadoReserva.APROVADA, responsavelId, "Aprovada pelo responsável");
    }

    @Transactional
    public Reserva rejeitar(Long reservaId, Long responsavelId) {
        return transitar(obter(reservaId), EstadoReserva.REJEITADA, responsavelId, "Rejeitada pelo responsável");
    }

    /** RF-09: retirada do material ou início do uso. */
    @Transactional
    public Reserva registrarRetirada(Long reservaId, Long responsavelId) {
        return transitar(obter(reservaId), EstadoReserva.EM_USO, responsavelId, "Retirada registrada");
    }

    /** RF-09: devolução do material ou fim do uso. */
    @Transactional
    public Reserva registrarDevolucao(Long reservaId, Long responsavelId) {
        return transitar(obter(reservaId), EstadoReserva.CONCLUIDA, responsavelId, "Devolução registrada");
    }

    @Transactional
    public Reserva registrarNaoComparecimento(Long reservaId, Long responsavelId) {
        return transitar(obter(reservaId), EstadoReserva.NAO_COMPARECEU, responsavelId, "Não compareceu");
    }

    /** RF-10: trilha completa da reserva, na ordem em que aconteceu. */
    @Transactional(readOnly = true)
    public List<AuditoriaReserva> historico(Long reservaId) {
        obter(reservaId);
        return auditoria.findByReservaIdOrderByIdAsc(reservaId);
    }

    @Transactional(readOnly = true)
    public List<Reserva> doUsuario(Long usuarioId) {
        return reservas.findBySolicitanteIdOrderByInicioAsc(usuarioId);
    }

    /** Reservas que tocam o dia (UTC). */
    @Transactional(readOnly = true)
    public List<Reserva> doDia(LocalDate dia) {
        Instant inicio = dia.atStartOfDay().toInstant(ZoneOffset.UTC);
        return reservas.queTocam(inicio, inicio.plusSeconds(24 * 60 * 60));
    }

    @Transactional(readOnly = true)
    public List<Reserva> aguardandoAprovacao() {
        return reservas.findByEstadoOrderByInicioAsc(EstadoReserva.SOLICITADA);
    }

    /** Recurso de uma reserva (usado para registrar o conflito evitado numa alteração recusada). */
    @Transactional(readOnly = true)
    public Long recursoDa(Long reservaId) {
        return obter(reservaId).getRecurso().getId();
    }

    /** RN-07 + RN-09: muda o estado e grava a auditoria na mesma transação. */
    private Reserva transitar(Reserva reserva, EstadoReserva novo, Long usuarioId, String motivo) {
        EstadoReserva anterior = reserva.getEstado();
        reserva.transitarPara(novo);
        Reserva salva = reservas.saveAndFlush(reserva);
        auditar(salva, usuarioId, anterior, novo, motivo);
        return salva;
    }

    private void auditar(Reserva reserva, Long usuarioId, EstadoReserva anterior, EstadoReserva novo, String motivo) {
        auditoria.save(new AuditoriaReserva(reserva.getId(), usuarioId, relogio.instant(), anterior, novo, motivo));
    }

    private Reserva obter(Long reservaId) {
        return reservas.findById(reservaId).orElseThrow(() -> new ReservaNaoEncontradaException(reservaId));
    }

    private static void exigirDono(Reserva reserva, Long usuarioId) {
        if (!reserva.pertenceA(usuarioId)) {
            throw new AcessoNegadoException("Você só pode alterar ou cancelar as próprias reservas.");
        }
    }

    private Recurso travarRecurso(Long recursoId) {
        return recursos.buscarTravandoParaReserva(recursoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(recursoId));
    }

    private void verificarDisponibilidade(Long recursoId, Intervalo intervalo, Long ignorar) {
        if (manutencoes.existeSobreposicao(recursoId, intervalo.inicio(), intervalo.termino())) {
            throw new RecursoEmManutencaoException();
        }
        boolean ocupado = ignorar == null
                ? reservas.existeSobreposicao(recursoId, intervalo.inicio(), intervalo.termino(), ESTADOS_QUE_OCUPAM)
                : reservas.existeSobreposicaoIgnorando(recursoId, intervalo.inicio(), intervalo.termino(),
                        ESTADOS_QUE_OCUPAM, ignorar);
        if (ocupado) {
            throw new ConflitoDeHorarioException();
        }
    }

    private Reserva gravar(Reserva reserva) {
        try {
            return reservas.saveAndFlush(reserva);
        } catch (DataIntegrityViolationException e) {
            if (violouProtecaoContraSobreposicao(e)) {
                throw new ConflitoDeHorarioException();
            }
            throw e;
        }
    }

    private static boolean violouProtecaoContraSobreposicao(DataIntegrityViolationException e) {
        String causa = e.getMostSpecificCause().getMessage();
        return causa != null && causa.contains(CONSTRAINT_SEM_SOBREPOSICAO);
    }
}
