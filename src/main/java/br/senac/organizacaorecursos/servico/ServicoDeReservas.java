package br.senac.organizacaorecursos.servico;

import br.senac.organizacaorecursos.dominio.Intervalo;
import br.senac.organizacaorecursos.dominio.Recurso;
import br.senac.organizacaorecursos.dominio.RecursoNaoEncontradoException;
import br.senac.organizacaorecursos.dominio.Reserva;
import br.senac.organizacaorecursos.persistencia.ManutencaoRepository;
import br.senac.organizacaorecursos.persistencia.RecursoRepository;
import br.senac.organizacaorecursos.persistencia.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDeReservas {

    private final RecursoRepository recursos;
    private final ReservaRepository reservas;
    private final ManutencaoRepository manutencoes;

    public ServicoDeReservas(RecursoRepository recursos, ReservaRepository reservas,
                             ManutencaoRepository manutencoes) {
        this.recursos = recursos;
        this.reservas = reservas;
        this.manutencoes = manutencoes;
    }

    @Transactional
    public Reserva solicitar(Long recursoId, Long solicitanteId, Intervalo intervalo) {
        Recurso recurso = recursos.findById(recursoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(recursoId));
        return reservas.saveAndFlush(Reserva.solicitar(recurso, solicitanteId, intervalo));
    }

    @Transactional
    public Reserva cancelar(Long reservaId) {
        Reserva reserva = reservas.findById(reservaId).orElseThrow();
        reserva.transitarPara(br.senac.organizacaorecursos.dominio.EstadoReserva.CANCELADA);
        return reserva;
    }
}
