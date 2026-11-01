package br.senac.organizacaorecursos.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import br.senac.organizacaorecursos.TestcontainersConfiguration;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/**
 * RF-13 / RNF-09, ponta a ponta: aplicação inteira numa porta aleatória, PostgreSQL real e um Chromium de verdade
 * (sem janela). Usa os usuários de demonstração, criados porque este contexto começa com o banco vazio.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class InterfaceE2EIT {

    private static Playwright playwright;
    private static Browser navegador;

    @LocalServerPort
    private int porta;

    private BrowserContext contexto;
    private Page pagina;

    @BeforeAll
    static void abrirNavegador() {
        playwright = Playwright.create();
        navegador = playwright.chromium().launch();
    }

    @AfterAll
    static void fecharNavegador() {
        navegador.close();
        playwright.close();
    }

    @BeforeEach
    void novaAba() {
        contexto = navegador.newContext(new Browser.NewContextOptions().setLocale("pt-BR"));
        pagina = contexto.newPage();
    }

    @AfterEach
    void fecharAba() {
        contexto.close();
    }

    @Test
    void solicitanteReservaSalaLivreERestritoAguardaAprovacaoDoResponsavel() {
        entrarComo("Solicitante");
        assertThat(pagina.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Agenda do dia"))).isVisible();

        pagina.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Nova reserva")).click();
        pagina.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Ver recursos livres")).click();
        reservar("Sala 101");
        assertThat(pagina.getByRole(AriaRole.STATUS)).containsText("Reserva criada: Sala 101");
        assertThat(pagina.getByRole(AriaRole.STATUS)).containsText("Aprovada");

        reservar("Laboratório 3");
        assertThat(pagina.getByRole(AriaRole.STATUS)).containsText("aguardando aprovação");

        pagina.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Minhas reservas")).click();
        assertThat(pagina.locator(".item").filter(new Locator.FilterOptions().setHasText("Laboratório 3")))
                .containsText("Solicitada");

        pagina.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sair")).click();
        entrarComo("Responsável");
        pagina.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Aprovações")).click();
        Locator pedido = pagina.locator(".item").filter(new Locator.FilterOptions().setHasText("Laboratório 3"));
        pedido.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Aprovar")).click();
        assertThat(pagina.getByText("Nenhuma reserva aguardando aprovação.")).isVisible();
    }

    @Test
    void loginComSenhaErradaMostraMensagemCompreensivel() {
        pagina.navigate(url("/"));
        pagina.getByLabel("E-mail").fill("solicitante@demo.invalid");
        pagina.getByLabel("Senha").fill("senha-errada");
        pagina.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Entrar")).click();

        assertThat(pagina.getByRole(AriaRole.ALERT)).hasText("E-mail ou senha incorretos.");
    }

    @Test
    void semLoginAAplicacaoVoltaParaATelaDeEntrada() {
        pagina.navigate(url("/app.html"));

        pagina.waitForURL(url("/"));
        assertThat(pagina.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Entrar"))).isVisible();
    }

    /** RNF-09: em largura de celular (360 px) a página não rola para o lado; a agenda rola dentro da própria caixa. */
    @Test
    void telasCabemEmLarguraDeCelularSemRolagemLateral() {
        pagina.setViewportSize(360, 740);
        pagina.navigate(url("/"));
        assertThat(semRolagemLateral()).as("tela de login").isTrue();

        entrarComo("Administrador");
        assertThat(pagina.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Agenda do dia"))).isVisible();
        assertThat(semRolagemLateral()).as("agenda").isTrue();
        pagina.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Recursos")).click();
        assertThat(pagina.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Cadastrados"))).isVisible();
        assertThat(semRolagemLateral()).as("recursos").isTrue();
    }

    private void entrarComo(String perfil) {
        pagina.navigate(url("/"));
        pagina.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(perfil).setExact(true)).click();
        pagina.getByLabel("Senha").fill("demo123");
        pagina.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Entrar")).click();
        pagina.waitForURL(url("/app.html"));
    }

    private void reservar(String recurso) {
        pagina.locator(".item").filter(new Locator.FilterOptions().setHasText(recurso))
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Reservar")).click();
    }

    private boolean semRolagemLateral() {
        return (Boolean) pagina.evaluate("() => document.documentElement.scrollWidth <= document.documentElement.clientWidth");
    }

    private String url(String caminho) {
        return "http://localhost:" + porta + caminho;
    }
}
