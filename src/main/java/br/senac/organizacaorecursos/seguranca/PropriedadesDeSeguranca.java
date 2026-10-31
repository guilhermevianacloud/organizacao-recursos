package br.senac.organizacaorecursos.seguranca;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** app.jwt.segredo (variável JWT_SEGREDO) e app.jwt.validade. */
@ConfigurationProperties(prefix = "app.jwt")
public record PropriedadesDeSeguranca(String segredo, Duration validade) {

    public PropriedadesDeSeguranca {
        if (validade == null) {
            validade = Duration.ofMinutes(60);
        }
    }
}
