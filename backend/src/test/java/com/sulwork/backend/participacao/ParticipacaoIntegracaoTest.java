package com.sulwork.backend.participacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sulwork.backend.TestcontainersConfiguration;
import com.sulwork.backend.comum.config.RelogioConfig;
import com.sulwork.backend.participante.cpf.CpfValidator;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class ParticipacaoIntegracaoTest {

    @Autowired
    MockMvc mockMvc;

    // o banco é compartilhado entre as classes de teste, então cada CPF precisa ser novo
    static String cpfAleatorio() {
        String base = String.format("%09d", ThreadLocalRandom.current().nextInt(1, 999_999_999));
        for (int dv = 0; dv < 100; dv++) {
            String cpf = base + String.format("%02d", dv);
            if (CpfValidator.valido(cpf)) {
                return cpf;
            }
        }
        return cpfAleatorio();
    }

    ResultActions postJson(String url, String corpo) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    long id(ResultActions resultado) throws Exception {
        return ((Number) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    long criarEvento(boolean abrir) throws Exception {
        var agora = LocalDateTime.now(RelogioConfig.FUSO).truncatedTo(ChronoUnit.MINUTES);
        long id = id(postJson("/api/eventos", """
                {"nome": "Café", "tipo": "CAFE_DA_MANHA", "dataHora": "%s", "prazoAlteracoes": "%s"}
                """.formatted(agora.plusDays(10), agora.plusDays(8))).andExpect(status().isCreated()));
        if (abrir) {
            mockMvc.perform(patch("/api/eventos/{id}/status", id)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"ABERTO\"}"))
                    .andExpect(status().isOk());
        }
        return id;
    }

    long criarParticipante(String cpf) throws Exception {
        return id(postJson("/api/participantes", """
                {"nome": "Ana", "cpf": "%s", "restricoes": ["VEGANO"]}
                """.formatted(cpf)).andExpect(status().isCreated()));
    }

    @Test
    void entrarDuasVezesNoMesmoEventoRetorna409() throws Exception {
        long evento = criarEvento(true);
        long participante = criarParticipante(cpfAleatorio());

        postJson("/api/eventos/" + evento + "/participantes/" + participante, "")
                .andExpect(status().isCreated());
        postJson("/api/eventos/" + evento + "/participantes/" + participante, "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("O participante já está neste evento."));
    }

    @Test
    void sairEVoltarReativaAMesmaParticipacao() throws Exception {
        long evento = criarEvento(true);
        long participante = criarParticipante(cpfAleatorio());
        String url = "/api/eventos/" + evento + "/participantes/" + participante;
        String lista = "/api/eventos/" + evento + "/participantes";

        long primeira = id(postJson(url, "").andExpect(status().isCreated()));
        mockMvc.perform(get(lista)).andExpect(jsonPath("$[*].id", hasItem((int) participante)));

        mockMvc.perform(delete(url)).andExpect(status().isNoContent());
        mockMvc.perform(get(lista)).andExpect(jsonPath("$[*].id", not(hasItem((int) participante))));

        long reativada = id(postJson(url, "").andExpect(status().isCreated()).andExpect(jsonPath("$.ativo").value(true)));
        mockMvc.perform(get(lista)).andExpect(jsonPath("$[*].id", hasItem((int) participante)));

        assertThat(reativada).isEqualTo(primeira);
    }

    @Test
    void entrarEmEventoRascunhoRetorna422() throws Exception {
        long evento = criarEvento(false);
        long participante = criarParticipante(cpfAleatorio());

        postJson("/api/eventos/" + evento + "/participantes/" + participante, "")
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void cpfComMascaraEhSalvoSoComNumerosEDuplicadoRetorna409() throws Exception {
        String cpf = cpfAleatorio();
        String mascarado = cpf.replaceFirst("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");

        postJson("/api/participantes", "{\"nome\": \"Bia\", \"cpf\": \"" + mascarado + "\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpf").value(cpf));
        postJson("/api/participantes", "{\"nome\": \"Outra\", \"cpf\": \"" + cpf + "\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void cpfInvalidoRetorna400NoCampoCpf() throws Exception {
        postJson("/api/participantes", "{\"nome\": \"Bia\", \"cpf\": \"11111111111\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("cpf"))
                .andExpect(jsonPath("$.campos[0].mensagem").value("CPF inválido."));
    }

    @Test
    void participanteExcluidoSaiDaListagemENaoEntraEmEvento() throws Exception {
        long evento = criarEvento(true);
        long participante = criarParticipante(cpfAleatorio());

        mockMvc.perform(delete("/api/participantes/{id}", participante)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/participantes")).andExpect(jsonPath("$[*].id", not(hasItem((int) participante))));
        postJson("/api/eventos/" + evento + "/participantes/" + participante, "")
                .andExpect(status().isNotFound());
    }
}
