package br.senac.organizacaorecursos.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import javax.crypto.SecretKey;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

/** RNF-05 / ADR-003: a chave de assinatura do JWT precisa de pelo menos 32 caracteres (fronteira 31/32/33). */
class ConfiguracaoDeSegurancaTest {

    private final ConfiguracaoDeSeguranca configuracao = new ConfiguracaoDeSeguranca();

    @Test
    void chaveCurtaDemaisImpedeASubida() {
        assertThatThrownBy(() -> configuracao.chaveDeAssinatura(propriedades("a".repeat(31))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 caracteres");
    }

    @ParameterizedTest(name = "chave com {0} caracteres é aceita")
    @ValueSource(ints = {32, 33})
    void chaveComTamanhoMinimoEhAceita(int tamanho) {
        SecretKey chave = configuracao.chaveDeAssinatura(propriedades("b".repeat(tamanho)));

        assertThat(chave.getEncoded()).hasSize(tamanho);
        assertThat(chave.getAlgorithm()).isEqualTo("HmacSHA256");
    }

    @Test
    void semSegredoConfiguradoGeraChaveAleatoriaDe32Bytes() {
        SecretKey primeira = configuracao.chaveDeAssinatura(propriedades(""));
        SecretKey segunda = configuracao.chaveDeAssinatura(propriedades(null));

        assertThat(primeira.getEncoded()).hasSize(32);
        assertThat(primeira.getEncoded()).isNotEqualTo(segunda.getEncoded());
    }

    private static PropriedadesDeSeguranca propriedades(String segredo) {
        return new PropriedadesDeSeguranca(segredo, Duration.ofMinutes(60));
    }
}
