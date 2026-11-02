package br.senac.organizacaorecursos.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.organizacaorecursos.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** RF-14: a documentação da API é pública e descreve os fluxos principais (antes só havia teste de fumaça manual). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DocumentacaoDaApiIT {

    @Autowired
    private MockMvc mvc;

    @Test
    void rf14_openApiPublicaDescreveOsFluxosPrincipaisSemLogin() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/api/reservas'].post").exists())
                .andExpect(jsonPath("$.paths['/api/reservas/{id}/aprovacao'].post").exists())
                .andExpect(jsonPath("$.paths['/api/recursos'].get").exists())
                .andExpect(jsonPath("$.paths['/api/manutencoes'].post").exists())
                .andExpect(jsonPath("$.paths['/api/relatorios/utilizacao'].get").exists());
    }

    @Test
    void rf14_telaDoSwaggerAbreSemLogin() throws Exception {
        mvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }
}
