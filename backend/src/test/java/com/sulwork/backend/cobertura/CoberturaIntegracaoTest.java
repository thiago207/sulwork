package com.sulwork.backend.cobertura;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sulwork.backend.TestcontainersConfiguration;
import com.sulwork.backend.comum.config.RelogioConfig;
import com.sulwork.backend.participante.cpf.CpfsDeTeste;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
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
class CoberturaIntegracaoTest {

    @Autowired
    MockMvc mockMvc;

    long evento;

    ResultActions enviar(MockHttpServletRequestBuilder req, String corpo) throws Exception {
        return mockMvc.perform(req.contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    long id(ResultActions resultado) throws Exception {
        return ((Number) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    long categoria(String nome, int meta, String unidade) throws Exception {
        return id(enviar(post("/api/eventos/{id}/categorias", evento), """
                {"nome": "%s", "meta": %d, "unidade": "%s"}""".formatted(nome, meta, unidade)));
    }

    long item(long categoria, String nome, int necessario, boolean obrigatorio, String alergenos) throws Exception {
        return id(enviar(post("/api/eventos/{id}/itens", evento), """
                {"nome": "%s", "categoriaId": %d, "quantidadeNecessaria": %d, "obrigatorio": %b, "alergenos": [%s]}
                """.formatted(nome, categoria, necessario, obrigatorio, alergenos)));
    }

    long participante(String restricao) throws Exception {
        long id = id(enviar(post("/api/participantes"), """
                {"nome": "Pessoa", "cpf": "%s", "restricoes": ["%s"]}""".formatted(CpfsDeTeste.aleatorio(), restricao)));
        enviar(post("/api/eventos/{e}/participantes/{p}", evento, id), "").andExpect(status().isCreated());
        return id;
    }

    long contribuir(long participante, long item, int quantidade) throws Exception {
        return id(enviar(post("/api/eventos/{id}/contribuicoes", evento), """
                {"participanteId": %d, "itemId": %d, "quantidade": %d}""".formatted(participante, item, quantidade))
                .andExpect(status().isCreated()));
    }

    // monta o exemplo numérico da regra 10, com duas contribuições que ficam inativas
    @BeforeEach
    void montarCenario() throws Exception {
        var agora = LocalDateTime.now(RelogioConfig.FUSO).truncatedTo(ChronoUnit.MINUTES);
        evento = id(enviar(post("/api/eventos"), """
                {"nome": "Café da manhã", "tipo": "CAFE_DA_MANHA", "dataHora": "%s", "prazoAlteracoes": "%s"}
                """.formatted(agora.plusDays(10), agora.plusDays(8))));
        long padaria = categoria("Padaria", 25, "UNIDADE");
        long bebidas = categoria("Bebidas", 6, "LITRO");
        categoria("Talheres", 10, "UNIDADE");

        long pao = item(padaria, "Pão", 20, true, "\"GLUTEN\"");
        long bolo = item(padaria, "Bolo", 2, false, "\"GLUTEN\", \"LACTOSE\", \"OVO\"");
        long cafe = item(bebidas, "Café", 3, true, "");
        long suco = item(bebidas, "Suco", 4, false, "");
        enviar(patch("/api/eventos/{id}/status", evento), "{\"status\": \"ABERTO\"}").andExpect(status().isOk());

        long ana = participante("VEGANO");
        long bruno = participante("SEM_LACTOSE");
        long carla = participante("SEM_GLUTEN");

        contribuir(ana, pao, 10);
        contribuir(bruno, pao, 5);
        contribuir(ana, cafe, 3);
        contribuir(bruno, suco, 1);

        // inativas: removida e de quem saiu do evento
        long removida = contribuir(ana, suco, 2);
        mockMvc.perform(delete("/api/eventos/{e}/contribuicoes/{c}", evento, removida)).andExpect(status().isNoContent());
        contribuir(carla, bolo, 2);
        mockMvc.perform(delete("/api/eventos/{e}/participantes/{p}", evento, carla)).andExpect(status().isNoContent());
    }

    @Test
    void coberturaSegueORegra10EIgnoraInativas() throws Exception {
        mockMvc.perform(get("/api/eventos/{id}/cobertura", evento))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coberturaGeral").value(62.5))
                .andExpect(jsonPath("$.obrigatoriosCompletos").value(1))
                .andExpect(jsonPath("$.obrigatoriosTotal").value(2))

                .andExpect(jsonPath("$.categorias", hasSize(3)))
                .andExpect(jsonPath("$.categorias[0].nome").value("Bebidas"))
                .andExpect(jsonPath("$.categorias[0].contribuido").value(4))
                .andExpect(jsonPath("$.categorias[0].unidade").value("LITRO"))
                .andExpect(jsonPath("$.categorias[0].percentual").value(66.7))
                .andExpect(jsonPath("$.categorias[1].nome").value("Padaria"))
                .andExpect(jsonPath("$.categorias[1].contribuido").value(15))
                .andExpect(jsonPath("$.categorias[1].meta").value(25))
                .andExpect(jsonPath("$.categorias[1].percentual").value(60.0))
                .andExpect(jsonPath("$.categorias[2].nome").value("Talheres"))
                .andExpect(jsonPath("$.categorias[2].percentual").value(0.0))

                .andExpect(jsonPath("$.itens", hasSize(4)))
                .andExpect(jsonPath("$.itens[0].nome").value("Café"))
                .andExpect(jsonPath("$.itens[0].percentual").value(100.0))
                .andExpect(jsonPath("$.itens[1].nome").value("Suco"))
                .andExpect(jsonPath("$.itens[1].contribuido").value(1))
                .andExpect(jsonPath("$.itens[1].faltante").value(3))
                .andExpect(jsonPath("$.itens[1].percentual").value(25.0))
                .andExpect(jsonPath("$.itens[2].nome").value("Bolo"))
                .andExpect(jsonPath("$.itens[2].contribuido").value(0))
                .andExpect(jsonPath("$.itens[3].nome").value("Pão"))
                .andExpect(jsonPath("$.itens[3].necessario").value(20))
                .andExpect(jsonPath("$.itens[3].contribuido").value(15))
                .andExpect(jsonPath("$.itens[3].faltante").value(5))
                .andExpect(jsonPath("$.itens[3].percentual").value(75.0));
    }

    @Test
    void restricoesContamSoParticipantesAtivos() throws Exception {
        mockMvc.perform(get("/api/eventos/{id}/restricoes", evento))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantesAtivos").value(2))
                .andExpect(jsonPath("$.resumo", hasSize(2)))
                .andExpect(jsonPath("$.resumo[*].restricao", containsInAnyOrder("VEGANO", "SEM_LACTOSE")))

                .andExpect(jsonPath("$.itens[?(@.nome == 'Bolo')].participantesAfetados").value(2))
                .andExpect(jsonPath("$.itens[?(@.nome == 'Bolo')].restricoesEmConflito[*]",
                        containsInAnyOrder("VEGANO", "SEM_LACTOSE")))
                .andExpect(jsonPath("$.itens[?(@.nome == 'Pão')].participantesAfetados").value(0))
                .andExpect(jsonPath("$.itens[?(@.nome == 'Café')].participantesAfetados").value(0));
    }

    @Test
    void eventoInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/eventos/{id}/cobertura", 999_999)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/eventos/{id}/restricoes", 999_999)).andExpect(status().isNotFound());
    }
}
