package br.senac.organizacaorecursos.api;

import br.senac.organizacaorecursos.api.Dtos.AlteracaoPedido;
import br.senac.organizacaorecursos.api.Dtos.AuditoriaResposta;
import br.senac.organizacaorecursos.dominio.AuditoriaReserva;
import br.senac.organizacaorecursos.dominio.Usuario;
import br.senac.organizacaorecursos.persistencia.UsuarioRepository;
import java.util.Map;
import java.util.stream.Collectors;
import br.senac.organizacaorecursos.api.Dtos.ReservaPedido;
import br.senac.organizacaorecursos.api.Dtos.ReservaResposta;
import br.senac.organizacaorecursos.dominio.Intervalo;
import br.senac.organizacaorecursos.seguranca.UsuarioAutenticado;
import br.senac.organizacaorecursos.servico.ServicoDeReservas;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** RF-04 a RF-09: RES-01..12 da matriz de autorização. A posse (RN-10) é conferida no serviço. */
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ServicoDeReservas servico;
    private final UsuarioRepository usuarios;

    public ReservaController(ServicoDeReservas servico, UsuarioRepository usuarios) {
        this.servico = servico;
        this.usuarios = usuarios;
    }

    /** RES-13: histórico só para Responsável e Administrador (RN-09). Só leitura: não há rota para alterar ou apagar. */
    @GetMapping("/{id}/historico")
    @PreAuthorize("hasAnyRole('RESPONSAVEL','ADMINISTRADOR')")
    public List<AuditoriaResposta> historico(@PathVariable Long id) {
        List<AuditoriaReserva> trilha = servico.historico(id);
        Map<Long, String> nomes = usuarios.findAllById(trilha.stream().map(AuditoriaReserva::getUsuarioId)
                        .collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Usuario::getId, Usuario::getNome));
        return trilha.stream().map(r -> new AuditoriaResposta(r.getInstante(), r.getUsuarioId(),
                nomes.get(r.getUsuarioId()), r.getEstadoAnterior(), r.getEstadoNovo(), r.getMotivo())).toList();
    }

    @GetMapping
    public List<ReservaResposta> doDia(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return servico.doDia(data).stream().map(ReservaResposta::de).toList();
    }

    @GetMapping("/minhas")
    @PreAuthorize("hasRole('SOLICITANTE')")
    public List<ReservaResposta> minhas(@AuthenticationPrincipal Jwt token) {
        return servico.doUsuario(UsuarioAutenticado.id(token)).stream().map(ReservaResposta::de).toList();
    }

    @GetMapping("/pendentes")
    @PreAuthorize("hasRole('RESPONSAVEL')")
    public List<ReservaResposta> pendentes() {
        return servico.aguardandoAprovacao().stream().map(ReservaResposta::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SOLICITANTE')")
    public ReservaResposta solicitar(@AuthenticationPrincipal Jwt token, @Valid @RequestBody ReservaPedido p) {
        return ReservaResposta.de(servico.solicitar(p.recursoId(), UsuarioAutenticado.id(token),
                new Intervalo(p.inicio(), p.termino()), p.motivo()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SOLICITANTE')")
    public ReservaResposta alterar(@AuthenticationPrincipal Jwt token, @PathVariable Long id,
                                   @Valid @RequestBody AlteracaoPedido p) {
        return ReservaResposta.de(servico.alterar(id, UsuarioAutenticado.id(token),
                new Intervalo(p.inicio(), p.termino())));
    }

    @PostMapping("/{id}/cancelamento")
    @PreAuthorize("hasRole('SOLICITANTE')")
    public ReservaResposta cancelar(@AuthenticationPrincipal Jwt token, @PathVariable Long id) {
        return ReservaResposta.de(servico.cancelar(id, UsuarioAutenticado.id(token)));
    }

    @PostMapping("/{id}/aprovacao")
    @PreAuthorize("hasRole('RESPONSAVEL')")
    public ReservaResposta aprovar(@AuthenticationPrincipal Jwt token, @PathVariable Long id) {
        return ReservaResposta.de(servico.aprovar(id, UsuarioAutenticado.id(token)));
    }

    @PostMapping("/{id}/rejeicao")
    @PreAuthorize("hasRole('RESPONSAVEL')")
    public ReservaResposta rejeitar(@AuthenticationPrincipal Jwt token, @PathVariable Long id) {
        return ReservaResposta.de(servico.rejeitar(id, UsuarioAutenticado.id(token)));
    }

    @PostMapping("/{id}/retirada")
    @PreAuthorize("hasRole('RESPONSAVEL')")
    public ReservaResposta retirada(@AuthenticationPrincipal Jwt token, @PathVariable Long id) {
        return ReservaResposta.de(servico.registrarRetirada(id, UsuarioAutenticado.id(token)));
    }

    @PostMapping("/{id}/devolucao")
    @PreAuthorize("hasRole('RESPONSAVEL')")
    public ReservaResposta devolucao(@AuthenticationPrincipal Jwt token, @PathVariable Long id) {
        return ReservaResposta.de(servico.registrarDevolucao(id, UsuarioAutenticado.id(token)));
    }

    @PostMapping("/{id}/nao-comparecimento")
    @PreAuthorize("hasRole('RESPONSAVEL')")
    public ReservaResposta naoCompareceu(@AuthenticationPrincipal Jwt token, @PathVariable Long id) {
        return ReservaResposta.de(servico.registrarNaoComparecimento(id, UsuarioAutenticado.id(token)));
    }
}
