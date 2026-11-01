package br.senac.organizacaorecursos.api;

import br.senac.organizacaorecursos.api.Dtos.LoginPedido;
import br.senac.organizacaorecursos.api.Dtos.TokenResposta;
import br.senac.organizacaorecursos.dominio.CredenciaisInvalidasException;
import br.senac.organizacaorecursos.dominio.Usuario;
import br.senac.organizacaorecursos.persistencia.UsuarioRepository;
import br.senac.organizacaorecursos.seguranca.ServicoDeToken;
import br.senac.organizacaorecursos.seguranca.ServicoDeToken.TokenEmitido;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AUT-01: login aberto a todos; devolve o JWT (ADR-003). */
@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;
    private final ServicoDeToken tokens;

    public AutenticacaoController(UsuarioRepository usuarios, PasswordEncoder codificador, ServicoDeToken tokens) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.tokens = tokens;
    }

    @PostMapping("/login")
    public TokenResposta entrar(@Valid @RequestBody LoginPedido pedido) {
        Usuario usuario = usuarios.findByEmail(pedido.email().trim().toLowerCase())
                .filter(u -> codificador.matches(pedido.senha(), u.getSenhaHash()))
                .orElseThrow(CredenciaisInvalidasException::new);
        TokenEmitido token = tokens.emitir(usuario);
        return new TokenResposta(token.valor(), "Bearer", token.expiraEm(), usuario.getPerfil().name(),
                usuario.getNome());
    }
}
