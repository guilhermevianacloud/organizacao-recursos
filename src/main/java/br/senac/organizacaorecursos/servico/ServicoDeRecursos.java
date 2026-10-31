package br.senac.organizacaorecursos.servico;

import br.senac.organizacaorecursos.dominio.Recurso;
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

    public ServicoDeRecursos(RecursoRepository recursos) {
        this.recursos = recursos;
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
}
