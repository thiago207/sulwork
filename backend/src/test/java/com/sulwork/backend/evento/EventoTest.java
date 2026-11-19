package com.sulwork.backend.evento;

import static com.sulwork.backend.evento.StatusEvento.ABERTO;
import static com.sulwork.backend.evento.StatusEvento.CANCELADO;
import static com.sulwork.backend.evento.StatusEvento.ENCERRADO;
import static com.sulwork.backend.evento.StatusEvento.RASCUNHO;
import static com.sulwork.backend.evento.StatusEvento.REALIZADO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sulwork.backend.comum.erro.RegraNegocioException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class EventoTest {

    static final LocalDateTime AGORA = LocalDateTime.of(2026, 12, 1, 10, 0);
    static final LocalDateTime DATA = AGORA.plusDays(10);
    static final LocalDateTime PRAZO = AGORA.plusDays(8);
    static final LocalDateTime DEPOIS_DO_PRAZO = PRAZO.plusHours(1);

    // escrita a partir da regra 1, sem reaproveitar o Map do enum
    static final Set<String> TRANSICOES_VALIDAS = Set.of(
            "RASCUNHO>ABERTO", "RASCUNHO>CANCELADO",
            "ABERTO>ENCERRADO", "ABERTO>CANCELADO",
            "ENCERRADO>REALIZADO", "ENCERRADO>ABERTO", "ENCERRADO>CANCELADO");

    static Evento novoEvento() {
        return new Evento("Café", "desc", TipoEvento.CAFE_DA_MANHA, DATA, PRAZO, AGORA);
    }

    static Evento eventoNoStatus(StatusEvento status) {
        var evento = novoEvento();
        var caminho = switch (status) {
            case RASCUNHO -> new StatusEvento[] {};
            case ABERTO -> new StatusEvento[] {ABERTO};
            case ENCERRADO -> new StatusEvento[] {ABERTO, ENCERRADO};
            case REALIZADO -> new StatusEvento[] {ABERTO, ENCERRADO, REALIZADO};
            case CANCELADO -> new StatusEvento[] {CANCELADO};
        };
        for (var passo : caminho) {
            evento.mudarStatus(passo, AGORA);
        }
        return evento;
    }

    static void editar(Evento evento, String descricao, LocalDateTime data, LocalDateTime prazo, LocalDateTime agora) {
        evento.editar(evento.getNome(), descricao, evento.getTipo(), data, prazo, agora);
    }

    @Nested
    class MaquinaDeStatus {

        static Stream<Arguments> todasAsCombinacoes() {
            return Arrays.stream(StatusEvento.values()).flatMap(origem -> Arrays.stream(StatusEvento.values())
                    .map(destino -> Arguments.of(origem, destino, TRANSICOES_VALIDAS.contains(origem + ">" + destino))));
        }

        @ParameterizedTest(name = "{0} -> {1}: válida = {2}")
        @MethodSource("todasAsCombinacoes")
        void respeitaATabelaDeTransicoes(StatusEvento origem, StatusEvento destino, boolean valida) {
            var evento = eventoNoStatus(origem);

            if (valida) {
                evento.mudarStatus(destino, AGORA);
                assertThat(evento.getStatus()).isEqualTo(destino);
            } else {
                assertThatThrownBy(() -> evento.mudarStatus(destino, AGORA))
                        .isInstanceOf(RegraNegocioException.class)
                        .hasMessage("Não é permitido mudar o status de " + origem + " para " + destino + ".");
                assertThat(evento.getStatus()).isEqualTo(origem);
            }
        }

        @Test
        void eventoNovoComecaEmRascunho() {
            assertThat(novoEvento().getStatus()).isEqualTo(RASCUNHO);
        }

        @Test
        void reabrirAntesDoPrazoEhPermitido() {
            var evento = eventoNoStatus(ENCERRADO);
            evento.mudarStatus(ABERTO, PRAZO.minusMinutes(1));
            assertThat(evento.getStatus()).isEqualTo(ABERTO);
        }

        @Test
        void reabrirComPrazoVencidoEhBloqueado() {
            var evento = eventoNoStatus(ENCERRADO);

            assertThatThrownBy(() -> evento.mudarStatus(ABERTO, DEPOIS_DO_PRAZO))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessage("Não é possível reabrir o evento: o prazo de alterações já venceu.");
            assertThat(evento.getStatus()).isEqualTo(ENCERRADO);
        }

        @Test
        void reabrirExatamenteNoPrazoEhBloqueado() {
            var evento = eventoNoStatus(ENCERRADO);
            assertThatThrownBy(() -> evento.mudarStatus(ABERTO, PRAZO)).isInstanceOf(RegraNegocioException.class);
        }

        @Test
        void prazoVencidoNaoImpedeEncerrarRealizarOuCancelar() {
            var aberto = eventoNoStatus(ABERTO);
            aberto.mudarStatus(ENCERRADO, DEPOIS_DO_PRAZO);
            aberto.mudarStatus(REALIZADO, DEPOIS_DO_PRAZO);
            assertThat(aberto.getStatus()).isEqualTo(REALIZADO);

            var encerrado = eventoNoStatus(ENCERRADO);
            encerrado.mudarStatus(CANCELADO, DEPOIS_DO_PRAZO);
            assertThat(encerrado.getStatus()).isEqualTo(CANCELADO);
        }

        @ParameterizedTest
        @EnumSource(value = StatusEvento.class, names = {"REALIZADO", "CANCELADO"})
        void statusFinaisNaoTemDestino(StatusEvento status) {
            assertThat(status.destinosPermitidos()).isEmpty();
        }
    }

    @Nested
    class DatasNaCriacao {

        @Test
        void aceitaDatasValidas() {
            var evento = novoEvento();
            assertThat(evento.getDataHora()).isEqualTo(DATA);
            assertThat(evento.getPrazoAlteracoes()).isEqualTo(PRAZO);
        }

        @Test
        void rejeitaDataNoPassado() {
            assertThatThrownBy(() -> new Evento("Café", null, TipoEvento.LANCHE, AGORA.minusDays(1), AGORA.minusDays(2), AGORA))
                    .hasMessage("A data e hora do evento deve estar no futuro.");
        }

        @Test
        void rejeitaDataIgualAAgora() {
            assertThatThrownBy(() -> new Evento("Café", null, TipoEvento.LANCHE, AGORA, AGORA.minusHours(1), AGORA))
                    .hasMessage("A data e hora do evento deve estar no futuro.");
        }

        @Test
        void rejeitaPrazoNoPassado() {
            assertThatThrownBy(() -> new Evento("Café", null, TipoEvento.LANCHE, DATA, AGORA.minusMinutes(1), AGORA))
                    .hasMessage("O prazo de alterações deve estar no futuro.");
        }

        @Test
        void rejeitaPrazoIgualADataDoEvento() {
            assertThatThrownBy(() -> new Evento("Café", null, TipoEvento.LANCHE, DATA, DATA, AGORA))
                    .hasMessage("O prazo de alterações deve ser anterior à data e hora do evento.");
        }

        @Test
        void rejeitaPrazoDepoisDaDataDoEvento() {
            assertThatThrownBy(() -> new Evento("Café", null, TipoEvento.LANCHE, DATA, DATA.plusHours(1), AGORA))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessage("O prazo de alterações deve ser anterior à data e hora do evento.");
        }
    }

    @Nested
    class Edicao {

        @ParameterizedTest
        @EnumSource(value = StatusEvento.class, names = {"RASCUNHO", "ABERTO"})
        void permiteEditarEmRascunhoEAberto(StatusEvento status) {
            var evento = eventoNoStatus(status);

            evento.editar("Almoço", "nova", TipoEvento.ALMOCO, DATA.plusDays(1), PRAZO.plusDays(1), AGORA);

            assertThat(evento.getNome()).isEqualTo("Almoço");
            assertThat(evento.getTipo()).isEqualTo(TipoEvento.ALMOCO);
            assertThat(evento.getStatus()).isEqualTo(status);
        }

        @ParameterizedTest
        @EnumSource(value = StatusEvento.class, names = {"ENCERRADO", "REALIZADO", "CANCELADO"})
        void bloqueiaEdicaoNosDemaisStatus(StatusEvento status) {
            var evento = eventoNoStatus(status);

            assertThatThrownBy(() -> editar(evento, "nova", DATA, PRAZO, AGORA))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("só pode ser editado com status RASCUNHO ou ABERTO");
        }

        @Test
        void editaDescricaoComPrazoVencidoSemMexerNasDatas() {
            var evento = eventoNoStatus(ABERTO);

            editar(evento, "descrição nova", DATA, PRAZO, DEPOIS_DO_PRAZO);

            assertThat(evento.getDescricao()).isEqualTo("descrição nova");
        }

        @Test
        void editaDescricaoMesmoDepoisDaDataDoEvento() {
            var evento = eventoNoStatus(ABERTO);

            editar(evento, "descrição nova", DATA, PRAZO, DATA.plusDays(1));

            assertThat(evento.getDescricao()).isEqualTo("descrição nova");
        }

        @Test
        void estendePrazoVencido() {
            var evento = eventoNoStatus(ABERTO);
            var novoPrazo = DATA.minusHours(2);

            editar(evento, "desc", DATA, novoPrazo, DEPOIS_DO_PRAZO);

            assertThat(evento.getPrazoAlteracoes()).isEqualTo(novoPrazo);
        }

        @Test
        void rejeitaNovoPrazoNoPassado() {
            var evento = eventoNoStatus(ABERTO);

            assertThatThrownBy(() -> editar(evento, "desc", DATA, PRAZO.plusMinutes(30), DEPOIS_DO_PRAZO))
                    .hasMessage("O prazo de alterações deve estar no futuro.");
            assertThat(evento.getPrazoAlteracoes()).isEqualTo(PRAZO);
        }

        @Test
        void rejeitaNovaDataNoPassado() {
            var evento = eventoNoStatus(RASCUNHO);

            assertThatThrownBy(() -> editar(evento, "desc", AGORA.minusDays(1), AGORA.minusDays(2), AGORA))
                    .hasMessage("A data e hora do evento deve estar no futuro.");
        }

        @Test
        void prazoAntesDaDataValeMesmoSemMudarAData() {
            var evento = eventoNoStatus(ABERTO);

            assertThatThrownBy(() -> editar(evento, "desc", DATA, DATA.plusHours(1), AGORA))
                    .hasMessage("O prazo de alterações deve ser anterior à data e hora do evento.");
            assertThat(evento.getDescricao()).isEqualTo("desc");
        }
    }

    @Nested
    class Exclusao {

        @Test
        void permiteExcluirEmRascunho() {
            novoEvento().validarExclusao();
        }

        @ParameterizedTest
        @EnumSource(value = StatusEvento.class, names = "RASCUNHO", mode = EnumSource.Mode.EXCLUDE)
        void bloqueiaExclusaoForaDeRascunho(StatusEvento status) {
            var evento = eventoNoStatus(status);

            assertThatThrownBy(evento::validarExclusao)
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("Só é possível excluir eventos com status RASCUNHO");
        }
    }
}
