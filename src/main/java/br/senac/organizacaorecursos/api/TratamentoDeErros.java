package br.senac.organizacaorecursos.api;

import br.senac.organizacaorecursos.dominio.AcessoNegadoException;
import br.senac.organizacaorecursos.dominio.CredenciaisInvalidasException;
import br.senac.organizacaorecursos.dominio.IntervaloInvalidoException;
import br.senac.organizacaorecursos.dominio.PesquisaInvalidaException;
import br.senac.organizacaorecursos.dominio.RecursoNaoEncontradoException;
import br.senac.organizacaorecursos.dominio.RegraDeNegocioException;
import br.senac.organizacaorecursos.dominio.ReservaNaoEncontradaException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Erro neutro (RN-12, RNF-06): toda resposta de erro é um ProblemDetail em português, com um código estável,
 * sem stack trace, SQL ou nome de classe interna.
 */
@RestControllerAdvice
public class TratamentoDeErros extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TratamentoDeErros.class);

    @ExceptionHandler(RegraDeNegocioException.class)
    ProblemDetail regraDeNegocio(RegraDeNegocioException e) {
        HttpStatus status = statusDe(e);
        return problema(status, tituloDe(status), e.getMessage(), e.getCodigo());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail acessoNegado(AccessDeniedException e) {
        return problema(HttpStatus.FORBIDDEN, "Acesso negado", "Seu perfil não permite esta operação.", "ACESSO_NEGADO");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail inesperado(Exception e) {
        LOG.error("Erro inesperado ao atender o pedido", e);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Não foi possível concluir o pedido. Tente novamente.", "ERRO_INTERNO");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
            HttpHeaders cabecalhos, HttpStatusCode status, WebRequest pedido) {
        String campos = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Corrija os campos: " + campos + ".", "DADOS_INVALIDOS"));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e,
            HttpHeaders cabecalhos, HttpStatusCode status, WebRequest pedido) {
        return ResponseEntity.badRequest().body(problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "O corpo do pedido não pôde ser lido. Confira o formato dos campos.", "CORPO_INVALIDO"));
    }

    private static HttpStatus statusDe(RegraDeNegocioException e) {
        if (e instanceof IntervaloInvalidoException || e instanceof PesquisaInvalidaException) {
            return HttpStatus.BAD_REQUEST;
        }
        if (e instanceof RecursoNaoEncontradoException || e instanceof ReservaNaoEncontradaException) {
            return HttpStatus.NOT_FOUND;
        }
        if (e instanceof AcessoNegadoException) {
            return HttpStatus.FORBIDDEN;
        }
        if (e instanceof CredenciaisInvalidasException) {
            return HttpStatus.UNAUTHORIZED;
        }
        return HttpStatus.CONFLICT;
    }

    private static String tituloDe(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Dados inválidos";
            case UNAUTHORIZED -> "Não autenticado";
            case NOT_FOUND -> "Não encontrado";
            case FORBIDDEN -> "Acesso negado";
            default -> "Conflito";
        };
    }

    static ProblemDetail problema(HttpStatus status, String titulo, String detalhe, String codigo) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        problema.setProperty("codigo", codigo);
        return problema;
    }
}
