package br.senac.organizacaorecursos.api;

import br.senac.organizacaorecursos.api.Dtos.RecursoPedido;
import br.senac.organizacaorecursos.api.Dtos.RecursoResposta;
import br.senac.organizacaorecursos.servico.ServicoDeRecursos;
import br.senac.organizacaorecursos.dominio.TipoRecurso;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
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

    /** RF-03: todos os filtros são opcionais; disponibilidade pede início e fim. */
    @GetMapping
    public List<RecursoResposta> pesquisar(
            @RequestParam(required = false) TipoRecurso tipo,
            @RequestParam(required = false) Integer capacidadeMinima,
            @RequestParam(required = false) String localizacao,
            @RequestParam(required = false) String competencia,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant disponivelInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant disponivelFim) {
        return servico.pesquisar(tipo, capacidadeMinima, localizacao, competencia, disponivelInicio, disponivelFim)
                .stream().map(RecursoResposta::de).toList();
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
