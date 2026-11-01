package br.senac.organizacaorecursos.seguranca;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Autenticação por JWT (ADR-003) e autorização por perfil.
 * A matriz perfil × operação está em docs/seguranca/matriz-autorizacao.md e é aplicada com @PreAuthorize nos controllers.
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(PropriedadesDeSeguranca.class)
public class ConfiguracaoDeSeguranca {

    private static final Logger LOG = LoggerFactory.getLogger(ConfiguracaoDeSeguranca.class);
    private static final int TAMANHO_MINIMO_DA_CHAVE = 32;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    @Bean
    SecurityFilterChain cadeiaDeSeguranca(HttpSecurity http, RespostasDeSeguranca respostas) throws Exception {
        http
                // Sem cookie de sessão: o token vai no cabeçalho Authorization, que o navegador não envia sozinho.
                // Sem esse envio automático não há CSRF a proteger (ADR-007).
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(regras -> regras
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/", "/index.html", "/app.html", "/css/**", "/js/**", "/img/**",
                                "/favicon.svg", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(o -> o
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDePerfil()))
                        .authenticationEntryPoint(respostas))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(respostas)
                        .accessDeniedHandler(respostas));
        return http.build();
    }

    /** O claim "perfil" vira a autoridade ROLE_SOLICITANTE, ROLE_RESPONSAVEL ou ROLE_ADMINISTRADOR. */
    private JwtAuthenticationConverter conversorDePerfil() {
        JwtGrantedAuthoritiesConverter perfis = new JwtGrantedAuthoritiesConverter();
        perfis.setAuthoritiesClaimName("perfil");
        perfis.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(perfis);
        return conversor;
    }

    @Bean
    SecretKey chaveDeAssinatura(PropriedadesDeSeguranca propriedades) {
        byte[] bytes;
        String segredo = propriedades.segredo();
        if (segredo == null || segredo.isBlank()) {
            bytes = new byte[TAMANHO_MINIMO_DA_CHAVE];
            ALEATORIO.nextBytes(bytes);
            LOG.info("JWT_SEGREDO não definido: chave aleatória gerada para esta execução.");
        } else {
            bytes = segredo.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < TAMANHO_MINIMO_DA_CHAVE) {
                throw new IllegalStateException("JWT_SEGREDO precisa ter pelo menos 32 caracteres.");
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder codificadorDeToken(SecretKey chave) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave));
    }

    /** Expiração conferida com o mesmo relógio injetado, sem tolerância. */
    @Bean
    JwtDecoder decodificadorDeToken(SecretKey chave, Clock relogio) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator validadeDoToken = new JwtTimestampValidator(Duration.ZERO);
        validadeDoToken.setClock(relogio);
        decodificador.setJwtValidator(validadeDoToken);
        return decodificador;
    }

    @Bean
    PasswordEncoder codificadorDeSenha() {
        return new BCryptPasswordEncoder();
    }
}
