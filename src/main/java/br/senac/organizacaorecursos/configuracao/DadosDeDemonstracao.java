package br.senac.organizacaorecursos.configuracao;

import br.senac.organizacaorecursos.dominio.Perfil;
import br.senac.organizacaorecursos.dominio.Recurso;
import br.senac.organizacaorecursos.dominio.TipoRecurso;
import br.senac.organizacaorecursos.dominio.Usuario;
import br.senac.organizacaorecursos.persistencia.RecursoRepository;
import br.senac.organizacaorecursos.persistencia.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria usuários e recursos fictícios para demonstrar o sistema, só quando o banco está vazio.
 * E-mails no domínio reservado .invalid; nenhum dado real (RNF-14).
 */
@Component
@ConditionalOnProperty(name = "app.demo.habilitado", havingValue = "true")
public class DadosDeDemonstracao implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DadosDeDemonstracao.class);

    private final UsuarioRepository usuarios;
    private final RecursoRepository recursos;
    private final PasswordEncoder codificador;
    private final String senha;

    public DadosDeDemonstracao(UsuarioRepository usuarios, RecursoRepository recursos, PasswordEncoder codificador,
                               @Value("${app.demo.senha}") String senha) {
        this.usuarios = usuarios;
        this.recursos = recursos;
        this.codificador = codificador;
        this.senha = senha;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        if (usuarios.count() > 0) {
            return;
        }
        String hash = codificador.encode(senha);
        usuarios.save(new Usuario("Marina (Solicitante)", "solicitante@demo.invalid", Perfil.SOLICITANTE, hash));
        usuarios.save(new Usuario("Rodrigo (Responsável)", "responsavel@demo.invalid", Perfil.RESPONSAVEL, hash));
        usuarios.save(new Usuario("Cláudia (Administradora)", "admin@demo.invalid", Perfil.ADMINISTRADOR, hash));

        recursos.save(recurso(TipoRecurso.SALA, "Sala 101", 40, "Bloco A", null, false));
        recursos.save(recurso(TipoRecurso.SALA, "Sala 205", 60, "Bloco B", null, false));
        recursos.save(recurso(TipoRecurso.SALA, "Laboratório 3", 30, "Bloco C", null, true));
        recursos.save(recurso(TipoRecurso.MATERIAL, "Projetor 7", null, "Bloco A", null, false));
        recursos.save(recurso(TipoRecurso.MATERIAL, "Notebook 12", null, "Almoxarifado", null, true));
        recursos.save(recurso(TipoRecurso.PROFESSOR, "Prof. Ana (fictícia)", null, null, "Banco de dados, Java", false));
        LOG.info("Dados de demonstração criados: 3 usuários (@demo.invalid) e 6 recursos.");
    }

    private static Recurso recurso(TipoRecurso tipo, String nome, Integer capacidade, String localizacao,
                                   String competencias, boolean restrito) {
        Recurso recurso = new Recurso(tipo, nome, restrito);
        recurso.setCapacidade(capacidade);
        recurso.setLocalizacao(localizacao);
        recurso.setCompetencias(competencias);
        return recurso;
    }
}
