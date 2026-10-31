package br.senac.organizacaorecursos.api;

import br.senac.organizacaorecursos.api.Dtos.ManutencaoPedido;
import br.senac.organizacaorecursos.api.Dtos.ManutencaoResposta;
import br.senac.organizacaorecursos.dominio.Intervalo;
import br.senac.organizacaorecursos.servico.ServicoDeManutencao;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** RF-08: MAN-01..02 da matriz de autorização. */
@RestController
@RequestMapping("/api/manutencoes")
public class ManutencaoController {

    private final ServicoDeManutencao servico;

    public ManutencaoController(ServicoDeManutencao servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<ManutencaoResposta> listar() {
        return servico.listar().stream().map(ManutencaoResposta::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ManutencaoResposta bloquear(@Valid @RequestBody ManutencaoPedido p) {
        return ManutencaoResposta.de(servico.bloquear(p.recursoId(), new Intervalo(p.inicio(), p.termino()), p.motivo()));
    }
}
