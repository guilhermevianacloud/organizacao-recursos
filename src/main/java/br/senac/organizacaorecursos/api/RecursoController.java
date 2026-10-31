package br.senac.organizacaorecursos.api;

import br.senac.organizacaorecursos.api.Dtos.RecursoPedido;
import br.senac.organizacaorecursos.api.Dtos.RecursoResposta;
import br.senac.organizacaorecursos.servico.ServicoDeRecursos;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** RF-02: REC-01..04 da matriz de autorização. */
@RestController
@RequestMapping("/api/recursos")
public class RecursoController {

    private final ServicoDeRecursos servico;

    public RecursoController(ServicoDeRecursos servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<RecursoResposta> listar() {
        return servico.listar().stream().map(RecursoResposta::de).toList();
    }

    @GetMapping("/{id}")
    public RecursoResposta obter(@PathVariable Long id) {
        return RecursoResposta.de(servico.obter(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public RecursoResposta cadastrar(@Valid @RequestBody RecursoPedido p) {
        return RecursoResposta.de(servico.cadastrar(p.tipo(), p.nome().trim(), p.capacidade(), p.localizacao(),
                p.competencias(), p.restrito()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public RecursoResposta alterar(@PathVariable Long id, @Valid @RequestBody RecursoPedido p) {
        return RecursoResposta.de(servico.alterar(id, p.tipo(), p.nome().trim(), p.capacidade(), p.localizacao(),
                p.competencias(), p.restrito()));
    }
}
