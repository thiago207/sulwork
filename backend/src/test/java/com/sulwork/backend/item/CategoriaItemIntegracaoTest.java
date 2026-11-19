package com.sulwork.backend.item;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class CategoriaItemIntegracaoTest {

    @Autowired
    MockMvc mockMvc;

    ResultActions enviar(MockHttpServletRequestBuilder req, String corpo) throws Exception {
        return mockMvc.perform(req.contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    long id(ResultActions resultado) throws Exception {
        return ((Number) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    long criarEvento() throws Exception {
        var agora = LocalDateTime.now(RelogioConfig.FUSO).truncatedTo(ChronoUnit.MINUTES);
        return id(enviar(post("/api/eventos"), """
                {"nome": "Café", "tipo": "CAFE_DA_MANHA", "dataHora": "%s", "prazoAlteracoes": "%s"}
                """.formatted(agora.plusDays(10), agora.plusDays(8))).andExpect(status().isCreated()));
    }

    void mudarStatus(long evento, String status) throws Exception {
        enviar(patch("/api/eventos/{id}/status", evento), "{\"status\": \"" + status + "\"}").andExpect(status().isOk());
    }

    long criarCategoria(long evento) throws Exception {
        return id(enviar(post("/api/eventos/{id}/categorias", evento),
                "{\"nome\": \"Bebidas\", \"meta\": 10, \"unidade\": \"LITRO\"}").andExpect(status().isCreated()));
    }

    String corpoItem(long categoria, int quantidade) {
        return """
                {"nome": "Suco", "categoriaId": %d, "quantidadeNecessaria": %d, "obrigatorio": true, "alergenos": ["MEL"]}
                """.formatted(categoria, quantidade);
    }

    @Test
    void itemHerdaUnidadeEEdicaoEhBloqueadaDepoisDeEncerrar() throws Exception {
        long evento = criarEvento();
        long categoria = criarCategoria(evento);

        long item = id(enviar(post("/api/eventos/{id}/itens", evento), corpoItem(categoria, 6))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unidade").value("LITRO"))
                .andExpect(jsonPath("$.categoriaNome").value("Bebidas"))
                .andExpect(jsonPath("$.alergenos[0]").value("MEL")));

        mockMvc.perform(get("/api/eventos/{id}/itens", evento))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].unidade").value("LITRO"));

        mudarStatus(evento, "ABERTO");
        enviar(put("/api/eventos/{e}/itens/{i}", evento, item), corpoItem(categoria, 8)).andExpect(status().isOk());

        mudarStatus(evento, "ENCERRADO");
        enviar(put("/api/eventos/{e}/itens/{i}", evento, item), corpoItem(categoria, 9))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void itemComCategoriaDeOutroEventoRetorna404() throws Exception {
        long eventoA = criarEvento();
        long eventoB = criarEvento();
        long categoriaDeA = criarCategoria(eventoA);

        enviar(post("/api/eventos/{id}/itens", eventoB), corpoItem(categoriaDeA, 1))
                .andExpect(status().isNotFound());
    }

    @Test
    void categoriaComItensNaoPodeSerExcluida() throws Exception {
        long evento = criarEvento();
        long categoria = criarCategoria(evento);
        enviar(post("/api/eventos/{id}/itens", evento), corpoItem(categoria, 1)).andExpect(status().isCreated());

        mockMvc.perform(delete("/api/eventos/{e}/categorias/{c}", evento, categoria))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void excluirEventoEmRascunhoLevaCategoriasEItens() throws Exception {
        long evento = criarEvento();
        long categoria = criarCategoria(evento);
        enviar(post("/api/eventos/{id}/itens", evento), corpoItem(categoria, 1)).andExpect(status().isCreated());

        mockMvc.perform(delete("/api/eventos/{id}", evento)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/eventos/{id}/itens", evento)).andExpect(status().isNotFound());
    }
}
