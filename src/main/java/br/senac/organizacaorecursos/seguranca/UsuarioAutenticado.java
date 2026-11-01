package br.senac.organizacaorecursos.seguranca;

import org.springframework.security.oauth2.jwt.Jwt;

/** Lê do token quem está fazendo o pedido. */
public final class UsuarioAutenticado {

    private UsuarioAutenticado() {
    }

    public static Long id(Jwt token) {
        return Long.valueOf(token.getSubject());
    }
}
