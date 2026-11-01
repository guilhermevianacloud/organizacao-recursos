package br.senac.organizacaorecursos.api;

import br.senac.organizacaorecursos.dominio.Intervalo;
import br.senac.organizacaorecursos.servico.ServicoDeRelatorios;
import br.senac.organizacaorecursos.servico.ServicoDeRelatorios.Relatorio;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** RF-12: REL-01 da matriz — só Responsável e Administrador. */
@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    private final ServicoDeRelatorios servico;

    public RelatorioController(ServicoDeRelatorios servico) {
        this.servico = servico;
    }

    @GetMapping("/utilizacao")
    @PreAuthorize("hasAnyRole('RESPONSAVEL','ADMINISTRADOR')")
    public Relatorio utilizacao(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant inicio,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fim) {
        return servico.utilizacao(new Intervalo(inicio, fim));
    }
}
