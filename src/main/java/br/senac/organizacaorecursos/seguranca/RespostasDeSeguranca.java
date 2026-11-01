package br.senac.organizacaorecursos.seguranca;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** 401 e 403 no formato ProblemDetail, sem detalhe interno (RNF-06). */
@Component
public class RespostasDeSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest pedido, HttpServletResponse resposta, AuthenticationException e)
            throws IOException {
        escrever(resposta, HttpStatus.UNAUTHORIZED, "Não autenticado", "Faça login para continuar.", "NAO_AUTENTICADO");
    }

    @Override
    public void handle(HttpServletRequest pedido, HttpServletResponse resposta, AccessDeniedException e)
            throws IOException {
        escrever(resposta, HttpStatus.FORBIDDEN, "Acesso negado",
                "Seu perfil não permite esta operação.", "ACESSO_NEGADO");
    }

    private static void escrever(HttpServletResponse resposta, HttpStatus status, String titulo, String detalhe,
                                 String codigo) throws IOException {
        resposta.setStatus(status.value());
        resposta.setContentType("application/problem+json");
        resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resposta.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + titulo + "\",\"status\":" + status.value()
                + ",\"detail\":\"" + detalhe + "\",\"codigo\":\"" + codigo + "\"}");
    }
}
