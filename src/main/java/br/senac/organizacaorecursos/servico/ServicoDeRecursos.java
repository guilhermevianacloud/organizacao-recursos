package br.senac.organizacaorecursos.servico;

import br.senac.organizacaorecursos.dominio.Intervalo;
import br.senac.organizacaorecursos.dominio.PesquisaInvalidaException;
import br.senac.organizacaorecursos.dominio.Recurso;
import br.senac.organizacaorecursos.persistencia.ManutencaoRepository;
import br.senac.organizacaorecursos.persistencia.ReservaRepository;
import java.time.Instant;
import java.util.Locale;
import br.senac.organizacaorecursos.dominio.RecursoNaoEncontradoException;
import br.senac.organizacaorecursos.dominio.TipoRecurso;
import br.senac.organizacaorecursos.persistencia.RecursoRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF-02: cadastro e consulta de salas, materiais e professores. */
@Service
public class ServicoDeRecursos {

    private final RecursoRepository recursos;
    private final ReservaRepository reservas;
    private final ManutencaoRepository manutencoes;

    public ServicoDeRecursos(RecursoRepository recursos, ReservaRepository reservas, ManutencaoRepository manutencoes) {
        this.recursos = recursos;
        this.reservas = reservas;
        this.manutencoes = manutencoes;
    }

    @Transactional
    public Recurso cadastrar(TipoRecurso tipo, String nome, Integer capacidade, String localizacao,
                             String competencias, boolean restrito) {
        Recurso recurso = new Recurso(tipo, nome, restrito);
        recurso.atualizar(tipo, nome, capacidade, localizacao, competencias, restrito);
        return recursos.save(recurso);
    }

    @Transactional
    public Recurso alterar(Long id, TipoRecurso tipo, String nome, Integer capacidade, String localizacao,
                           String competencias, boolean restrito) {
        Recurso recurso = obter(id);
        recurso.atualizar(tipo, nome, capacidade, localizacao, competencias, restrito);
        return recursos.save(recurso);
    }

    @Transactional(readOnly = true)
    public Recurso obter(Long id) {
        return recursos.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException(id));
    }

    @Transactional(readOnly = true)
    public List<Recurso> listar() {
        return recursos.findAll(Sort.by("tipo", "nome"));
    }

    /**
     * RF-03: filtros opcionais e combináveis. Disponibilidade exige início e fim: exclui recurso com reserva
     * que ocupa ou manutenção no intervalo (mesmo critério semiaberto do ADR-004).
     */
    @Transactional(readOnly = true)
    public List<Recurso> pesquisar(TipoRecurso tipo, Integer capacidadeMinima, String localizacao,
                                   String competencia, Instant disponivelInicio, Instant disponivelFim) {
        if ((disponivelInicio == null) != (disponivelFim == null)) {
            throw new PesquisaInvalidaException("Para pesquisar disponibilidade, informe o início e o fim.");
        }
        Intervalo disponivel = disponivelInicio == null ? null : new Intervalo(disponivelInicio, disponivelFim);
        return listar().stream()
                .filter(Recurso::isAtivo)
                .filter(r -> tipo == null || r.getTipo() == tipo)
                .filter(r -> capacidadeMinima == null || (r.getCapacidade() != null && r.getCapacidade() >= capacidadeMinima))
                .filter(r -> contem(r.getLocalizacao(), localizacao))
                .filter(r -> contem(r.getCompetencias(), competencia))
                .filter(r -> disponivel == null || estaLivre(r, disponivel))
                .toList();
    }

    private boolean estaLivre(Recurso recurso, Intervalo intervalo) {
        return !reservas.existeSobreposicao(recurso.getId(), intervalo.inicio(), intervalo.termino(),
                ServicoDeReservas.ESTADOS_QUE_OCUPAM)
                && !manutencoes.existeSobreposicao(recurso.getId(), intervalo.inicio(), intervalo.termino());
    }

    private static boolean contem(String valor, String trecho) {
        if (trecho == null || trecho.isBlank()) {
            return true;
        }
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(trecho.trim().toLowerCase(Locale.ROOT));
    }
}
