package com.sulwork.backend.sugestao;

import static org.hamcrest.Matchers.contains;
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
class SugestaoIntegracaoTest {

    @Autowired
    MockMvc mockMvc;

    long evento;
    long ana;
    long carla;
    long semEvento;

    ResultActions enviar(MockHttpServletRequestBuilder req, String corpo) throws Exception {
        return mockMvc.perform(req.contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    long id(ResultActions resultado) throws Exception {
        return ((Number) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    long item(long categoria, String nome, int necessario, boolean obrigatorio, String alergenos) throws Exception {
        return id(enviar(post("/api/eventos/{id}/itens", evento), """
                {"nome": "%s", "categoriaId": %d, "quantidadeNecessaria": %d, "obrigatorio": %b, "alergenos": [%s]}
                """.formatted(nome, categoria, necessario, obrigatorio, alergenos)));
    }

    long participante(String restricoes) throws Exception {
        return id(enviar(post("/api/participantes"), """
                {"nome": "Pessoa", "cpf": "%s", "restricoes": [%s]}""".formatted(CpfsDeTeste.aleatorio(), restricoes)));
    }

    void entrar(long participante) throws Exception {
        enviar(post("/api/eventos/{e}/participantes/{p}", evento, participante), "").andExpect(status().isCreated());
    }

    void contribuir(long participante, long item, int quantidade) throws Exception {
        enviar(post("/api/eventos/{id}/contribuicoes", evento), """
                {"participanteId": %d, "itemId": %d, "quantidade": %d}""".formatted(participante, item, quantidade))
                .andExpect(status().isCreated());
    }

    // Ana é vegana, Bruno sem lactose, Carla sem restrição
    @BeforeEach
    void montarCenario() throws Exception {
        var agora = LocalDateTime.now(RelogioConfig.FUSO).truncatedTo(ChronoUnit.MINUTES);
        evento = id(enviar(post("/api/eventos"), """
                {"nome": "Café", "tipo": "CAFE_DA_MANHA", "dataHora": "%s", "prazoAlteracoes": "%s"}
                """.formatted(agora.plusDays(10), agora.plusDays(8))));
        long categoria = id(enviar(post("/api/eventos/{id}/categorias", evento),
                "{\"nome\": \"Mesa\", \"meta\": 50, \"unidade\": \"UNIDADE\"}"));

        long pao = item(categoria, "Pão", 10, true, "\"GLUTEN\"");
        long cafe = item(categoria, "Café", 3, true, "");
        item(categoria, "Leite", 2, true, "\"LACTOSE\"");
        item(categoria, "Bolo", 4, false, "\"GLUTEN\", \"LACTOSE\", \"OVO\"");
        item(categoria, "Mel", 2, false, "\"MEL\"");
        long suco = item(categoria, "Suco", 4, false, "");
        enviar(patch("/api/eventos/{id}/status", evento), "{\"status\": \"ABERTO\"}").andExpect(status().isOk());

        ana = participante("\"VEGANO\"");
        long bruno = participante("\"SEM_LACTOSE\"");
        carla = participante("");
        semEvento = participante("");
        entrar(ana);
        entrar(bruno);
        entrar(carla);

        contribuir(ana, cafe, 3);
        contribuir(bruno, pao, 5);
        contribuir(bruno, suco, 1);
        contribuir(carla, suco, 1);
    }

    @Test
    void sugereNaOrdemEOcultaQuemConflitaComTodosComRestricao() throws Exception {
        // Café completo sai; Leite e Bolo conflitam com Ana e Bruno e saem; Mel conflita só com Ana
        mockMvc.perform(get("/api/eventos/{e}/participantes/{p}/sugestoes", evento, carla))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ocultosPorConflito").value(2))
                .andExpect(jsonPath("$.sugestoes[*].item", contains("Pão", "Mel", "Suco")))

                .andExpect(jsonPath("$.sugestoes[0].obrigatorio").value(true))
                .andExpect(jsonPath("$.sugestoes[0].faltante").value(5))
                .andExpect(jsonPath("$.sugestoes[0].cobertura").value(50.0))
                .andExpect(jsonPath("$.sugestoes[0].categoria").value("Mesa"))
                .andExpect(jsonPath("$.sugestoes[0].unidade").value("UNIDADE"))

                .andExpect(jsonPath("$.sugestoes[1].conflitos.participantes").value(1))
                .andExpect(jsonPath("$.sugestoes[1].conflitos.restricoes", contains("VEGANO")))
                .andExpect(jsonPath("$.sugestoes[1].conflitos.comVoce").value(false))

                .andExpect(jsonPath("$.sugestoes[2].cobertura").value(50.0))
                .andExpect(jsonPath("$.sugestoes[2].suaContribuicao").value(1));
    }

    @Test
    void quemTemARestricaoVeOItemMarcadoComoConflitoProprio() throws Exception {
        mockMvc.perform(get("/api/eventos/{e}/participantes/{p}/sugestoes", evento, ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sugestoes[1].item").value("Mel"))
                .andExpect(jsonPath("$.sugestoes[1].conflitos.comVoce").value(true));
    }

    @Test
    void participanteForaDoEventoRecebe422() throws Exception {
        mockMvc.perform(get("/api/eventos/{e}/participantes/{p}/sugestoes", evento, semEvento))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("O participante não está ativo neste evento."));
    }

    @Test
    void eventoQueNaoAceitaContribuicoesRetorna422() throws Exception {
        enviar(patch("/api/eventos/{id}/status", evento), "{\"status\": \"ENCERRADO\"}").andExpect(status().isOk());

        mockMvc.perform(get("/api/eventos/{e}/participantes/{p}/sugestoes", evento, carla))
                .andExpect(status().isUnprocessableContent());
    }
}
