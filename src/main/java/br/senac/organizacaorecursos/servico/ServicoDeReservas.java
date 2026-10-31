package br.senac.organizacaorecursos.servico;

import br.senac.organizacaorecursos.dominio.ConflitoDeHorarioException;
import br.senac.organizacaorecursos.dominio.EstadoReserva;
import br.senac.organizacaorecursos.dominio.Intervalo;
import br.senac.organizacaorecursos.dominio.Recurso;
import br.senac.organizacaorecursos.dominio.RecursoEmManutencaoException;
import br.senac.organizacaorecursos.dominio.RecursoNaoEncontradoException;
import br.senac.organizacaorecursos.dominio.Reserva;
import br.senac.organizacaorecursos.persistencia.ManutencaoRepository;
import br.senac.organizacaorecursos.persistencia.RecursoRepository;
import br.senac.organizacaorecursos.persistencia.ReservaRepository;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDeReservas {

    /** Estados que ocupam o recurso (RN-02): só eles entram na verificação de sobreposição. */
    static final Set<EstadoReserva> ESTADOS_QUE_OCUPAM = Arrays.stream(EstadoReserva.values())
            .filter(EstadoReserva::ocupaRecurso)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(EstadoReserva.class)));

    /** Nome da exclusion constraint criada na migração V3. */
    static final String CONSTRAINT_SEM_SOBREPOSICAO = "reserva_sem_sobreposicao";

    private final RecursoRepository recursos;
    private final ReservaRepository reservas;
    private final ManutencaoRepository manutencoes;

    public ServicoDeReservas(RecursoRepository recursos, ReservaRepository reservas,
                             ManutencaoRepository manutencoes) {
        this.recursos = recursos;
        this.reservas = reservas;
        this.manutencoes = manutencoes;
    }

    /**
     * RN-04 e RN-02: recusa se o recurso estiver em manutenção ou já ocupado no intervalo.
     * RN-03 em duas camadas (ADR-002):
     * 1. a linha do recurso é travada, então pedidos pelo mesmo recurso são atendidos em fila
     *    e a verificação abaixo enxerga a reserva de quem chegou antes (sem deadlock, DEF-002);
     * 2. a constraint reserva_sem_sobreposicao garante a regra para qualquer caminho de escrita.
     */
    @Transactional
    public Reserva solicitar(Long recursoId, Long solicitanteId, Intervalo intervalo) {
        Recurso recurso = recursos.buscarTravandoParaReserva(recursoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(recursoId));
        if (manutencoes.existeSobreposicao(recursoId, intervalo.inicio(), intervalo.termino())) {
            throw new RecursoEmManutencaoException();
        }
        if (reservas.existeSobreposicao(recursoId, intervalo.inicio(), intervalo.termino(), ESTADOS_QUE_OCUPAM)) {
            throw new ConflitoDeHorarioException();
        }
        try {
            return reservas.saveAndFlush(Reserva.solicitar(recurso, solicitanteId, intervalo));
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

    /** Cancelar muda o estado (RN-07); nunca apaga a reserva (RN-08). */
    @Transactional
    public Reserva cancelar(Long reservaId) {
        Reserva reserva = reservas.findById(reservaId).orElseThrow();
        reserva.transitarPara(EstadoReserva.CANCELADA);
        return reserva;
    }
}
