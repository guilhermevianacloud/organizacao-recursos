package br.senac.organizacaorecursos.dominio;

/**
 * Violação de regra de negócio. O código estável (ex.: "INTERVALO_INVALIDO")
 * permite testar o motivo sem depender do texto da mensagem.
 */
public class RegraDeNegocioException extends RuntimeException {

    private final String codigo;

    public RegraDeNegocioException(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
