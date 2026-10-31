package br.senac.organizacaorecursos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Marco zero (Aula 03): a aplicação inteira sobe contra um PostgreSQL real
 * e o Flyway aplica as migrações. Verificado no próprio banco, não só pelo "não deu erro".
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OrganizacaoRecursosApplicationTests {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void aplicacaoSobeEmPostgresRealComMigracaoInicialAplicada() {
        String banco = jdbc.queryForObject("select version()", String.class);
        Integer migracoesComSucesso = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where version = '1' and success",
                Integer.class);

        assertThat(banco).startsWith("PostgreSQL 17");
        assertThat(migracoesComSucesso).isEqualTo(1);
    }
}
