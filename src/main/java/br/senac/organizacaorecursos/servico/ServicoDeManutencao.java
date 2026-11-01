package br.senac.organizacaorecursos.servico;

import br.senac.organizacaorecursos.dominio.Intervalo;
import br.senac.organizacaorecursos.dominio.Manutencao;
import br.senac.organizacaorecursos.dominio.ManutencaoConflitaComReservaException;
import br.senac.organizacaorecursos.dominio.Recurso;
import br.senac.organizacaorecursos.dominio.RecursoNaoEncontradoException;
import br.senac.organizacaorecursos.persistencia.ManutencaoRepository;
import br.senac.organizacaorecursos.persistencia.RecursoRepository;
import br.senac.organizacaorecursos.persistencia.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDeManutencao {

    private final RecursoRepository recursos;
    private final ManutencaoRepository manutencoes;
    private final ReservaRepository reservas;

    public ServicoDeManutencao(RecursoRepository recursos, ManutencaoRepository manutencoes,
                               ReservaRepository reservas) {
        this.recursos = recursos;
        this.manutencoes = manutencoes;
        this.reservas = reservas;
    }

    /** RN-04 com a política provisória da Q-02: não bloqueia período que já tem reserva ativa. */
    @Transactional
    public Manutencao bloquear(Long recursoId, Intervalo intervalo, String motivo) {
        Recurso recurso = recursos.buscarTravandoParaReserva(recursoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(recursoId));
        if (reservas.existeSobreposicao(recursoId, intervalo.inicio(), intervalo.termino(),
                ServicoDeReservas.ESTADOS_QUE_OCUPAM)) {
            throw new ManutencaoConflitaComReservaException();
        }
        return manutencoes.save(new Manutencao(recurso, intervalo, motivo));
    }
}
