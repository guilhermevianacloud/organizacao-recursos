package br.senac.organizacaorecursos.configuracao;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** O tempo é uma dependência injetada: em produção o relógio real, nos testes um relógio fixo (Aula 07). */
@Configuration
public class ConfiguracaoDeTempo {

    @Bean
    public Clock relogio() {
        return Clock.systemUTC();
    }
}
