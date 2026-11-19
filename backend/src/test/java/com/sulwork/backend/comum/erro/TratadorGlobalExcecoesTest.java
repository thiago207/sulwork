package com.sulwork.backend.comum.erro;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class TratadorGlobalExcecoesTest {

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ControllerDeTeste())
                .setControllerAdvice(new TratadorGlobalExcecoes())
                .build();
    }

    @Test
    void naoEncontradoRetorna404() throws Exception {
        mockMvc.perform(get("/teste/nao-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.erro").value("Não encontrado"))
                .andExpect(jsonPath("$.mensagem").value("Evento 1 não encontrado."))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.campos").isEmpty());
    }

    @Test
    void conflitoRetorna409() throws Exception {
        mockMvc.perform(get("/teste/conflito"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("CPF já cadastrado."));
    }

    @Test
    void regraNegocioRetorna422() throws Exception {
        mockMvc.perform(get("/teste/regra"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.erro").value("Regra de negócio violada"))
                .andExpect(jsonPath("$.mensagem").value("Prazo de alterações encerrado."));
    }

    @Test
    void validacaoRetorna400ComCampos() throws Exception {
        mockMvc.perform(post("/teste/validacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("nome"))
                .andExpect(jsonPath("$.campos[0].mensagem").value("O nome é obrigatório."));
    }

    @Test
    void jsonMalformadoRetorna400() throws Exception {
        mockMvc.perform(post("/teste/validacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{nome"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Corpo da requisição inválido ou malformado."));
    }

    @Test
    void parametroComTipoErradoRetorna400() throws Exception {
        mockMvc.perform(get("/teste/numero/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Valor inválido para o parâmetro 'id'."));
    }

    @Test
    void metodoNaoSuportadoRetorna405() throws Exception {
        mockMvc.perform(post("/teste/conflito"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void erroInesperadoRetorna500SemVazarDetalhes() throws Exception {
        mockMvc.perform(get("/teste/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.mensagem").value("Erro interno inesperado."));
    }

    record EntradaTeste(@NotBlank(message = "O nome é obrigatório.") String nome) {
    }

    @RestController
    static class ControllerDeTeste {

        @GetMapping("/teste/nao-encontrado")
        void naoEncontrado() {
            throw new NaoEncontradoException("Evento 1 não encontrado.");
        }

        @GetMapping("/teste/conflito")
        void conflito() {
            throw new ConflitoException("CPF já cadastrado.");
        }

        @GetMapping("/teste/regra")
        void regra() {
            throw new RegraNegocioException("Prazo de alterações encerrado.");
        }

        @PostMapping("/teste/validacao")
        void validacao(@Valid @RequestBody EntradaTeste entrada) {
        }

        @GetMapping("/teste/numero/{id}")
        void numero(@PathVariable Long id) {
        }

        @GetMapping("/teste/inesperado")
        void inesperado() {
            throw new IllegalStateException("detalhe interno que não deve vazar");
        }
    }
}
