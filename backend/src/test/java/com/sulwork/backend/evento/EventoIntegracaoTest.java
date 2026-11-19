package com.sulwork.backend.evento;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sulwork.backend.TestcontainersConfiguration;
import com.sulwork.backend.comum.config.RelogioConfig;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class EventoIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    private static String corpoEvento(String nome) {
        LocalDateTime agora = LocalDateTime.now(RelogioConfig.FUSO).truncatedTo(ChronoUnit.MINUTES);
        return """
                {
                  "nome": "%s",
                  "descricao": "Cada um leva uma coisa",
                  "tipo": "CAFE_DA_MANHA",
                  "dataHora": "%s",
                  "prazoAlteracoes": "%s"
                }
                """.formatted(nome, agora.plusDays(10), agora.plusDays(8));
    }

    private static String corpoStatus(StatusEvento status) {
        return "{\"status\": \"" + status + "\"}";
    }

    private long criarEvento(String nome) throws Exception {
        String resposta = mockMvc.perform(post("/api/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoEvento(nome)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
    }

    private void mudarStatus(long id, StatusEvento destino) throws Exception {
        mockMvc.perform(patch("/api/eventos/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoStatus(destino)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(destino.name()));
    }

    @Test
    void fluxoCriarAbrirEncerrarReabrir() throws Exception {
        // criar
        String resposta = mockMvc.perform(post("/api/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoEvento("Café de fim de ano")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/eventos/")))
                .andExpect(jsonPath("$.status").value("RASCUNHO"))
                .andExpect(jsonPath("$.nome").value("Café de fim de ano"))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(resposta, "$.id")).longValue();

        // abrir, encerrar e reabrir
        mudarStatus(id, StatusEvento.ABERTO);
        mudarStatus(id, StatusEvento.ENCERRADO);
        mudarStatus(id, StatusEvento.ABERTO);

        // o status ficou gravado no banco
        mockMvc.perform(get("/api/eventos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABERTO"));

        // e o filtro por status encontra o evento
        mockMvc.perform(get("/api/eventos").param("status", "ABERTO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem((int) id)));
    }

    @Test
    void transicaoInvalidaRetorna422() throws Exception {
        long id = criarEvento("Lanche");

        mockMvc.perform(patch("/api/eventos/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoStatus(StatusEvento.REALIZADO)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("Não é permitido mudar o status de RASCUNHO para REALIZADO."));
    }

    @Test
    void criarComPrazoDepoisDaDataRetorna422() throws Exception {
        LocalDateTime agora = LocalDateTime.now(RelogioConfig.FUSO).truncatedTo(ChronoUnit.MINUTES);
        String corpo = """
                {"nome": "Almoço", "tipo": "ALMOCO", "dataHora": "%s", "prazoAlteracoes": "%s"}
                """.formatted(agora.plusDays(5), agora.plusDays(6));

        mockMvc.perform(post("/api/eventos").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("O prazo de alterações deve ser anterior à data e hora do evento."));
    }

    @Test
    void criarSemCamposObrigatoriosRetorna400() throws Exception {
        mockMvc.perform(post("/api/eventos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("nome")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("dataHora")));
    }

    @Test
    void excluirSoEmRascunho() throws Exception {
        long aberto = criarEvento("Aberto");
        mudarStatus(aberto, StatusEvento.ABERTO);
        mockMvc.perform(delete("/api/eventos/{id}", aberto))
                .andExpect(status().isUnprocessableContent());

        long rascunho = criarEvento("Rascunho");
        mockMvc.perform(delete("/api/eventos/{id}", rascunho))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/eventos/{id}", rascunho))
                .andExpect(status().isNotFound());
    }
}
