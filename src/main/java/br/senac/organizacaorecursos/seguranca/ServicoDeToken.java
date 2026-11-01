package br.senac.organizacaorecursos.seguranca;

import br.senac.organizacaorecursos.dominio.Usuario;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Emite o JWT do usuário com o perfil e a expiração (ADR-003). */
@Service
public class ServicoDeToken {

    private final JwtEncoder codificador;
    private final Clock relogio;
    private final Duration validade;

    public ServicoDeToken(JwtEncoder codificador, Clock relogio, PropriedadesDeSeguranca propriedades) {
        this.codificador = codificador;
        this.relogio = relogio;
        this.validade = propriedades.validade();
    }

    public TokenEmitido emitir(Usuario usuario) {
        Instant agora = relogio.instant();
        Instant expiraEm = agora.plus(validade);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(usuario.getId().toString())
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .claim("perfil", usuario.getPerfil().name())
                .claim("nome", usuario.getNome())
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String valor = codificador.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new TokenEmitido(valor, expiraEm);
    }

    public record TokenEmitido(String valor, Instant expiraEm) {
    }
}
