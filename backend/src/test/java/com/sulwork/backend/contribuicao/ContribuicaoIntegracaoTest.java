package com.sulwork.backend.contribuicao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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
import com.sulwork.backend.participante.cpf.CpfsDeTeste;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
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
class ContribuicaoIntegracaoTest {

    @Autowired
    MockMvc mockMvc;

    ResultActions enviar(MockHttpServletRequestBuilder req, String corpo) throws Exception {
        return mockMvc.perform(req.contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    long id(ResultActions resultado) throws Exception {
        return ((Number) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    long criarEventoAberto() throws Exception {
        var agora = LocalDateTime.now(RelogioConfig.FUSO).truncatedTo(ChronoUnit.MINUTES);
        long evento = id(enviar(post("/api/eventos"), """
                {"nome": "Café", "tipo": "CAFE_DA_MANHA", "dataHora": "%s", "prazoAlteracoes": "%s"}
                """.formatted(agora.plusDays(10), agora.plusDays(8))).andExpect(status().isCreated()));
        // o evento só abre com pelo menos um item
        long apoio = id(enviar(post("/api/eventos/{id}/categorias", evento),
                "{\"nome\": \"Apoio\", \"meta\": 1, \"unidade\": \"UNIDADE\"}"));
        enviar(post("/api/eventos/{id}/itens", evento),
                "{\"nome\": \"Guardanapo\", \"categoriaId\": %d, \"quantidadeNecessaria\": 1}".formatted(apoio));
        enviar(patch("/api/eventos/{id}/status", evento), "{\"status\": \"ABERTO\"}").andExpect(status().isOk());
        return evento;
    }

    long criarItem(long evento, int necessario, String alergenos) throws Exception {
        long categoria = id(enviar(post("/api/eventos/{id}/categorias", evento),
                "{\"nome\": \"Bebidas\", \"meta\": 50, \"unidade\": \"LITRO\"}").andExpect(status().isCreated()));
        return id(enviar(post("/api/eventos/{id}/itens", evento), """
                {"nome": "Leite", "categoriaId": %d, "quantidadeNecessaria": %d, "alergenos": [%s]}
                """.formatted(categoria, necessario, alergenos)).andExpect(status().isCreated()));
    }

    long participanteNoEvento(long evento, String restricoes) throws Exception {
        long participante = id(enviar(post("/api/participantes"), """
                {"nome": "Pessoa", "cpf": "%s", "restricoes": [%s]}
                """.formatted(CpfsDeTeste.aleatorio(), restricoes)).andExpect(status().isCreated()));
        enviar(post("/api/eventos/{e}/participantes/{p}", evento, participante), "").andExpect(status().isCreated());
        return participante;
    }

    ResultActions contribuir(long evento, long participante, long item, int quantidade) throws Exception {
        return enviar(post("/api/eventos/{id}/contribuicoes", evento), """
                {"participanteId": %d, "itemId": %d, "quantidade": %d}
                """.formatted(participante, item, quantidade));
    }

    @Test
    void duasPessoasCompletamOItemEATerceiraRecebe422() throws Exception {
        long evento = criarEventoAberto();
        long item = criarItem(evento, 5, "");
        long ana = participanteNoEvento(evento, "");
        long bruno = participanteNoEvento(evento, "");
        long carla = participanteNoEvento(evento, "");

        contribuir(evento, ana, item, 3).andExpect(status().isCreated());
        contribuir(evento, bruno, item, 2).andExpect(status().isCreated());
        contribuir(evento, carla, item, 1)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("A quantidade ultrapassa o necessário para o item Leite. Faltam 0."));

        mockMvc.perform(get("/api/eventos/{id}/contribuicoes", evento)).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void contribuicoesSimultaneasNaoPassamDoNecessario() throws Exception {
        long evento = criarEventoAberto();
        long item = criarItem(evento, 5, "");
        List<Long> participantes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            participantes.add(participanteNoEvento(evento, ""));
        }

        // as 10 threads esperam a largada e disparam juntas
        var largada = new CountDownLatch(1);
        List<Callable<Integer>> tarefas = participantes.stream().<Callable<Integer>>map(p -> () -> {
            largada.await();
            return contribuir(evento, p, item, 1).andReturn().getResponse().getStatus();
        }).toList();

        List<Integer> statusRecebidos = new ArrayList<>();
        try (var executor = Executors.newFixedThreadPool(10)) {
            var futuros = tarefas.stream().map(executor::submit).toList();
            largada.countDown();
            for (var futuro : futuros) {
                statusRecebidos.add(futuro.get());
            }
        }

        assertThat(statusRecebidos).filteredOn(s -> s == 201).hasSize(5);
        assertThat(statusRecebidos).filteredOn(s -> s == 422).hasSize(5);
        mockMvc.perform(get("/api/eventos/{id}/contribuicoes", evento)).andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    void sairDesativaAsContribuicoesEVoltarNaoAsReativa() throws Exception {
        long evento = criarEventoAberto();
        long item = criarItem(evento, 5, "");
        long ana = participanteNoEvento(evento, "");
        contribuir(evento, ana, item, 4).andExpect(status().isCreated());

        mockMvc.perform(delete("/api/eventos/{e}/participantes/{p}", evento, ana)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/eventos/{id}/contribuicoes", evento)).andExpect(jsonPath("$", hasSize(0)));

        enviar(post("/api/eventos/{e}/participantes/{p}", evento, ana), "").andExpect(status().isCreated());
        mockMvc.perform(get("/api/eventos/{id}/contribuicoes", evento)).andExpect(jsonPath("$", hasSize(0)));

        // a quantidade liberada volta a ficar disponível
        contribuir(evento, ana, item, 5).andExpect(status().isCreated());
    }

    @Test
    void naoReduzQuantidadeNecessariaAbaixoDoContribuido() throws Exception {
        long evento = criarEventoAberto();
        long item = criarItem(evento, 5, "");
        long ana = participanteNoEvento(evento, "");
        contribuir(evento, ana, item, 3).andExpect(status().isCreated());
        long categoria = ((Number) JsonPath.read(mockMvc.perform(get("/api/eventos/{e}/itens/{i}", evento, item))
                .andReturn().getResponse().getContentAsString(), "$.categoriaId")).longValue();
        String corpo = "{\"nome\": \"Leite\", \"categoriaId\": %d, \"quantidadeNecessaria\": %d}";

        enviar(put("/api/eventos/{e}/itens/{i}", evento, item), corpo.formatted(categoria, 2))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem", containsString("total já contribuído (3)")));
        enviar(put("/api/eventos/{e}/itens/{i}", evento, item), corpo.formatted(categoria, 3))
                .andExpect(status().isOk());
    }

    @Test
    void contribuicaoComAlergenoConflitanteTrazAvisoMasEhCriada() throws Exception {
        long evento = criarEventoAberto();
        long item = criarItem(evento, 5, "\"LACTOSE\"");
        long ana = participanteNoEvento(evento, "\"SEM_LACTOSE\"");

        contribuir(evento, ana, item, 1)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.avisos[0]").value("O item Leite contém lactose, que conflita com sua restrição: sem lactose."));

        mockMvc.perform(get("/api/participantes/{id}/contribuicoes", ana))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].unidade").value("LITRO"));
    }
}
