package com.sulwork.backend.verificacao;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, ConsistenciaEventoIntegracaoTest.RelogioDeTeste.class})
@ActiveProfiles("test")
class ConsistenciaEventoIntegracaoTest {

    // relógio que dá para adiantar; @Primary vence o Clock do RelogioConfig só neste contexto de teste
    static class RelogioMutavel extends Clock {
        private static final Instant INICIO = Instant.parse("2035-05-15T15:00:00Z");
        private volatile Instant agora = INICIO;

        @Override
        public ZoneId getZone() {
            return RelogioConfig.FUSO;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }

        void adiantar(Duration tempo) {
            agora = agora.plus(tempo);
        }

        void reiniciar() {
            agora = INICIO;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RelogioDeTeste {
        @Bean
        @Primary
        RelogioMutavel relogioMutavel() {
            return new RelogioMutavel();
        }
    }

    @Autowired
    MockMvc mockMvc;
    @Autowired
    RelogioMutavel relogio;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    TransactionTemplate transacao;
    @Autowired
    DataSource banco;

    @BeforeEach
    void zerarRelogio() {
        relogio.reiniciar();
    }

    ResultActions enviar(MockHttpServletRequestBuilder req, String corpo) throws Exception {
        return mockMvc.perform(req.contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    long id(ResultActions resultado) throws Exception {
        return ((Number) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    long criarEvento() throws Exception {
        var agora = LocalDateTime.now(relogio);
        return id(enviar(post("/api/eventos"), """
                {"nome": "Verificação", "tipo": "LANCHE", "dataHora": "%s", "prazoAlteracoes": "%s"}
                """.formatted(agora.plusDays(2), agora.plusDays(1))).andExpect(status().isCreated()));
    }

    long criarCategoria(long evento, String nome) throws Exception {
        return id(enviar(post("/api/eventos/{id}/categorias", evento),
                "{\"nome\": \"%s\", \"meta\": 10, \"unidade\": \"UNIDADE\"}".formatted(nome))
                .andExpect(status().isCreated()));
    }

    long criarItem(long evento, long categoria) throws Exception {
        return id(enviar(post("/api/eventos/{id}/itens", evento),
                "{\"nome\": \"Coxinha\", \"categoriaId\": %d, \"quantidadeNecessaria\": 10}".formatted(categoria))
                .andExpect(status().isCreated()));
    }

    void mudarStatus(long evento, String destino) throws Exception {
        enviar(patch("/api/eventos/{id}/status", evento), "{\"status\": \"" + destino + "\"}").andExpect(status().isOk());
    }

    long participanteNoEvento(long evento) throws Exception {
        long participante = id(enviar(post("/api/participantes"),
                "{\"nome\": \"Maria\", \"cpf\": \"%s\"}".formatted(CpfsDeTeste.aleatorio())).andExpect(status().isCreated()));
        enviar(post("/api/eventos/{e}/participantes/{p}", evento, participante), "").andExpect(status().isCreated());
        return participante;
    }

    ResultActions contribuir(long evento, long participante, long item, int quantidade) throws Exception {
        return enviar(post("/api/eventos/{id}/contribuicoes", evento),
                "{\"participanteId\": %d, \"itemId\": %d, \"quantidade\": %d}".formatted(participante, item, quantidade));
    }

    // outra transação segura o lock da linha até soltar; quem precisar dela fica esperando
    CountDownLatch segurarLock(String sql, long id, CountDownLatch travado) {
        var soltar = new CountDownLatch(1);
        CompletableFuture.runAsync(() -> transacao.executeWithoutResult(t -> {
            jdbc.queryForList(sql, id);
            travado.countDown();
            try {
                soltar.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
        return soltar;
    }

    // espera até N conexões estarem paradas aguardando lock no PostgreSQL
    void esperarConexoesBloqueadas(int quantidade) throws InterruptedException {
        for (int i = 0; i < 200; i++) {
            Integer bloqueadas = jdbc.queryForObject(
                    "select count(*) from pg_stat_activity where wait_event_type = 'Lock'", Integer.class);
            if (bloqueadas != null && bloqueadas >= quantidade) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("nenhuma transação ficou esperando o lock");
    }

    int statusDe(ResultActions resultado) {
        return resultado.andReturn().getResponse().getStatus();
    }

    @Test
    void participanteExcluidoDepoisDoEventoRealizadoSaiDosIndicadores() throws Exception {
        long evento = criarEvento();
        long item = criarItem(evento, criarCategoria(evento, "Salgados"));
        mudarStatus(evento, "ABERTO");
        long maria = participanteNoEvento(evento);
        contribuir(evento, maria, item, 4).andExpect(status().isCreated());
        mudarStatus(evento, "ENCERRADO");
        relogio.adiantar(Duration.ofDays(3));
        mudarStatus(evento, "REALIZADO");

        // a regra 6 só bloqueia exclusão em evento ABERTO ou ENCERRADO; REALIZADO deixa excluir
        mockMvc.perform(delete("/api/participantes/{id}", maria)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/eventos/{id}/participantes", evento)).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/eventos/{id}/restricoes", evento)).andExpect(jsonPath("$.participantesAtivos").value(0));
        mockMvc.perform(get("/api/eventos/{id}/contribuicoes", evento))
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/eventos/{id}/cobertura", evento))
                .andExpect(jsonPath("$.itens[0].contribuido").value(0))
                .andExpect(jsonPath("$.categorias[0].contribuido").value(0));
    }

    @Test
    void migracaoDesativaContribuicoesLegadasComVinculosInativos() throws Exception {
        long evento = criarEvento();
        long item = criarItem(evento, criarCategoria(evento, "Salgados"));
        mudarStatus(evento, "ABERTO");
        long participanteExcluido = participanteNoEvento(evento);
        long participanteQueSaiu = participanteNoEvento(evento);
        contribuir(evento, participanteExcluido, item, 4).andExpect(status().isCreated());
        contribuir(evento, participanteQueSaiu, item, 4).andExpect(status().isCreated());

        jdbc.update("update participante set ativo = false where id = ?", participanteExcluido);
        jdbc.update("update participacao set ativo = false where evento_id = ? and participante_id = ?",
                evento, participanteQueSaiu);
        new ResourceDatabasePopulator(new ClassPathResource(
                "db/migration/V7__desativar_vinculos_de_participantes_excluidos.sql")).execute(banco);

        assertThat(jdbc.queryForObject("select count(*) from contribuicao where item_id = ? and ativo",
                Long.class, item)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from participacao where evento_id = ? and ativo",
                Long.class, evento)).isZero();
        enviar(post("/api/eventos/{e}/participantes/{p}", evento, participanteQueSaiu), "")
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/eventos/{id}/cobertura", evento))
                .andExpect(jsonPath("$.itens[0].contribuido").value(0));
    }

    @Test
    void saidaConcorrenteDesativaContribuicaoAntesDaReentrada() throws Exception {
        long evento = criarEvento();
        long item = criarItem(evento, criarCategoria(evento, "Salgados"));
        mudarStatus(evento, "ABERTO");
        long maria = participanteNoEvento(evento);

        var travado = new CountDownLatch(1);
        var soltar = segurarLock("select id from item where id = ? for update", item, travado);
        assertThat(travado.await(10, TimeUnit.SECONDS)).isTrue();

        var contribuicao = contribuirEmParalelo(evento, maria, item);
        CompletableFuture<Integer> saida;
        try {
            esperarConexoesBloqueadas(1);
            saida = CompletableFuture.supplyAsync(() -> {
                try {
                    return statusDe(mockMvc.perform(delete("/api/eventos/{e}/participantes/{p}", evento, maria)));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            esperarConexoesBloqueadas(2);
        } finally {
            soltar.countDown();
        }
        assertThat(contribuicao.get(15, TimeUnit.SECONDS)).isEqualTo(201);
        assertThat(saida.get(15, TimeUnit.SECONDS)).isEqualTo(204);

        var estado = jdbc.queryForMap("""
                select c.ativo as contribuicao_ativa, p.ativo as participacao_ativa
                from contribuicao c join participacao p on p.id = c.participacao_id
                where c.item_id = ?""", item);
        assertThat(estado).containsEntry("contribuicao_ativa", false).containsEntry("participacao_ativa", false);
        mockMvc.perform(get("/api/eventos/{id}/cobertura", evento)).andExpect(jsonPath("$.itens[0].contribuido").value(0));

        enviar(post("/api/eventos/{e}/participantes/{p}", evento, maria), "").andExpect(status().isCreated());
        mockMvc.perform(get("/api/eventos/{id}/cobertura", evento)).andExpect(jsonPath("$.itens[0].contribuido").value(0));
        mockMvc.perform(get("/api/eventos/{id}/contribuicoes", evento)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void contribuicaoEmEsperaRecebe422QuandoEncerramentoPrevalece() throws Exception {
        long evento = criarEvento();
        long item = criarItem(evento, criarCategoria(evento, "Salgados"));
        mudarStatus(evento, "ABERTO");
        long maria = participanteNoEvento(evento);

        var contribuicao = transacao.execute(t -> {
            jdbc.queryForList("select id from evento where id = ? for update", evento);
            var futura = contribuirEmParalelo(evento, maria, item);
            try {
                esperarConexoesBloqueadas(1);
                mudarStatus(evento, "ENCERRADO");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return futura;
        });
        assertThat(contribuicao.get(15, TimeUnit.SECONDS)).isEqualTo(422);
        mockMvc.perform(get("/api/eventos/{id}", evento)).andExpect(jsonPath("$.status").value("ENCERRADO"));
        mockMvc.perform(get("/api/eventos/{id}/cobertura", evento)).andExpect(jsonPath("$.itens[0].contribuido").value(0));
    }

    @Test
    void contribuicaoEmEsperaRecebe422QuandoAtingeOPrazo() throws Exception {
        long evento = criarEvento();
        long item = criarItem(evento, criarCategoria(evento, "Salgados"));
        mudarStatus(evento, "ABERTO");
        long maria = participanteNoEvento(evento);

        var travado = new CountDownLatch(1);
        var soltar = segurarLock("select id from item where id = ? for update", item, travado);
        CompletableFuture<Integer> contribuicao;
        try {
            assertThat(travado.await(10, TimeUnit.SECONDS)).isTrue();
            contribuicao = contribuirEmParalelo(evento, maria, item);
            esperarConexoesBloqueadas(1);
            relogio.adiantar(Duration.ofDays(1));
        } finally {
            soltar.countDown();
        }
        assertThat(contribuicao.get(15, TimeUnit.SECONDS)).isEqualTo(422);
    }

    CompletableFuture<Integer> contribuirEmParalelo(long evento, long participante, long item) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return statusDe(contribuir(evento, participante, item, 4));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void criacaoConcorrenteDeCategoriaDuplicadaRecebe409() throws Exception {
        long evento = criarEvento();

        var travado = new CountDownLatch(1);
        var soltar = segurarLock("select id from evento where id = ? for update", evento, travado);
        assertThat(travado.await(10, TimeUnit.SECONDS)).isTrue();

        var primeira = CompletableFuture.supplyAsync(() -> criarCategoriaSemConferir(evento, "Bebidas"));
        var segunda = CompletableFuture.supplyAsync(() -> criarCategoriaSemConferir(evento, "bÉbidas"));
        try {
            esperarConexoesBloqueadas(2);
        } finally {
            soltar.countDown();
        }
        assertThat(List.of(primeira.get(15, TimeUnit.SECONDS), segunda.get(15, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(201, 409);
        mockMvc.perform(get("/api/eventos/{id}/categorias", evento)).andExpect(jsonPath("$", hasSize(1)));
    }

    int criarCategoriaSemConferir(long evento, String nome) {
        try {
            return statusDe(enviar(post("/api/eventos/{id}/categorias", evento),
                    "{\"nome\": \"%s\", \"meta\": 10, \"unidade\": \"LITRO\"}".formatted(nome)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
