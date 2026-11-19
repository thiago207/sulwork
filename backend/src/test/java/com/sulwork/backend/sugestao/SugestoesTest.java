package com.sulwork.backend.sugestao;

import static com.sulwork.backend.item.Alergeno.GLUTEN;
import static com.sulwork.backend.item.Alergeno.LACTOSE;
import static com.sulwork.backend.item.Alergeno.MEL;
import static com.sulwork.backend.participante.RestricaoAlimentar.SEM_GLUTEN;
import static com.sulwork.backend.participante.RestricaoAlimentar.SEM_LACTOSE;
import static com.sulwork.backend.participante.RestricaoAlimentar.VEGANO;
import static org.assertj.core.api.Assertions.assertThat;

import com.sulwork.backend.categoria.UnidadeMedida;
import com.sulwork.backend.cobertura.CoberturaPonderada;
import com.sulwork.backend.item.Alergeno;
import com.sulwork.backend.participante.RestricaoAlimentar;
import com.sulwork.backend.sugestao.Sugestoes.Candidato;
import com.sulwork.backend.sugestao.Sugestoes.Resultado;
import com.sulwork.backend.sugestao.dto.SugestoesResposta.Sugestao;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SugestoesTest {

    static long proximoId = 1;

    static Candidato item(String nome, boolean obrigatorio, int necessario, long contribuido, Alergeno... alergenos) {
        return new Candidato(proximoId++, nome, "Geral", UnidadeMedida.UNIDADE, obrigatorio, necessario, contribuido,
                Set.of(alergenos), 0);
    }

    static Resultado montar(List<Candidato> itens, List<Set<RestricaoAlimentar>> ativos, Set<RestricaoAlimentar> minhas) {
        return Sugestoes.montar(itens, ativos, minhas, new CoberturaPonderada());
    }

    static Resultado semRestricoes(Candidato... itens) {
        return montar(List.of(itens), List.of(Set.of(), Set.of()), Set.of());
    }

    static List<String> nomes(Resultado r) {
        return r.sugestoes().stream().map(Sugestao::item).toList();
    }

    @Nested
    class Ordenacao {

        @Test
        void obrigatoriosVemPrimeiroMesmoComCoberturaMaior() {
            var r = semRestricoes(item("Suco", false, 10, 0), item("Pão", true, 10, 9));

            assertThat(nomes(r)).containsExactly("Pão", "Suco");
        }

        @Test
        void dentroDoMesmoGrupoMenorCoberturaPrimeiro() {
            var r = semRestricoes(
                    item("Café", true, 4, 3),
                    item("Pão", true, 10, 2),
                    item("Leite", true, 2, 1),
                    item("Bolo", false, 5, 4),
                    item("Suco", false, 4, 0));

            assertThat(nomes(r)).containsExactly("Pão", "Leite", "Café", "Suco", "Bolo");
        }

        @Test
        void mesmaCoberturaDesempataPeloMaiorFaltante() {
            var r = semRestricoes(item("Copos", false, 2, 0), item("Pratos", false, 10, 0));

            assertThat(nomes(r)).containsExactly("Pratos", "Copos");
        }

        @Test
        void empateTotalOrdenaPeloNomeIgnorandoAcento() {
            var r = semRestricoes(item("Suco", false, 4, 0), item("Água", false, 4, 0), item("Leite", false, 4, 0));

            assertThat(nomes(r)).containsExactly("Água", "Leite", "Suco");
        }

        @Test
        void sugestaoTrazFaltanteECobertura() {
            var sugestao = semRestricoes(item("Pão", true, 20, 15)).sugestoes().getFirst();

            assertThat(sugestao.faltante()).isEqualTo(5);
            assertThat(sugestao.cobertura()).hasToString("75.0");
        }
    }

    @Nested
    class Filtro {

        @Test
        void itemCompletoNaoAparece() {
            var r = semRestricoes(item("Café", true, 3, 3), item("Pão", true, 10, 0));

            assertThat(nomes(r)).containsExactly("Pão");
            assertThat(r.ocultosPorConflito()).isZero();
        }

        @Test
        void conflitoComTodosQueTemRestricaoExcluiEConta() {
            // os dois que têm restrição não podem leite; o terceiro não tem restrição
            var ativos = List.of(Set.of(VEGANO), Set.of(SEM_LACTOSE), Set.<RestricaoAlimentar>of());
            var r = montar(List.of(item("Leite", true, 2, 0, LACTOSE), item("Pão", true, 10, 0, GLUTEN)), ativos, Set.of());

            assertThat(nomes(r)).containsExactly("Pão");
            assertThat(r.ocultosPorConflito()).isEqualTo(1);
        }

        @Test
        void conflitoComAlgunsApareceMarcado() {
            var ativos = List.of(Set.of(VEGANO), Set.of(SEM_LACTOSE), Set.of(SEM_GLUTEN));
            var r = montar(List.of(item("Mel", false, 2, 0, MEL), item("Queijo", false, 2, 0, LACTOSE)), ativos, Set.of());

            var mel = r.sugestoes().stream().filter(s -> s.item().equals("Mel")).findFirst().orElseThrow();
            assertThat(mel.conflitos().participantes()).isEqualTo(1);
            assertThat(mel.conflitos().restricoes()).containsExactly(VEGANO);

            var queijo = r.sugestoes().stream().filter(s -> s.item().equals("Queijo")).findFirst().orElseThrow();
            assertThat(queijo.conflitos().participantes()).isEqualTo(2);
            assertThat(queijo.conflitos().restricoes()).containsExactly(VEGANO, SEM_LACTOSE);
            assertThat(r.ocultosPorConflito()).isZero();
        }

        @Test
        void semNinguemComRestricaoNadaEhExcluido() {
            var r = semRestricoes(item("Leite", true, 2, 0, LACTOSE), item("Pão", true, 10, 0, GLUTEN));

            assertThat(r.sugestoes()).hasSize(2);
            assertThat(r.sugestoes()).allMatch(s -> s.conflitos().participantes() == 0 && s.conflitos().restricoes().isEmpty());
        }

        @Test
        void itemSemAlergenosNuncaConflita() {
            var ativos = List.of(Set.of(VEGANO, SEM_GLUTEN), Set.of(SEM_LACTOSE));
            var r = montar(List.of(item("Água", false, 5, 0)), ativos, Set.of(VEGANO));

            assertThat(r.sugestoes()).singleElement().satisfies(s -> {
                assertThat(s.conflitos().participantes()).isZero();
                assertThat(s.conflitos().comVoce()).isFalse();
            });
        }

        @Test
        void marcaQuandoConflitaComQuemPede() {
            var ativos = List.of(Set.of(VEGANO), Set.of(SEM_GLUTEN));
            var r = montar(List.of(item("Mel", false, 2, 0, MEL)), ativos, Set.of(VEGANO));

            assertThat(r.sugestoes().getFirst().conflitos().comVoce()).isTrue();
        }

        @Test
        void comUmaSoPessoaComRestricaoOQueElaNaoPodeSai() {
            var ativos = List.of(Set.of(SEM_GLUTEN), Set.<RestricaoAlimentar>of(), Set.<RestricaoAlimentar>of());
            var r = montar(List.of(item("Pão", true, 10, 0, GLUTEN), item("Suco", false, 4, 0)), ativos, Set.of());

            assertThat(nomes(r)).containsExactly("Suco");
            assertThat(r.ocultosPorConflito()).isEqualTo(1);
        }

        @Test
        void listaVaziaQuandoNaoHaItens() {
            var r = semRestricoes();

            assertThat(r.sugestoes()).isEmpty();
            assertThat(r.ocultosPorConflito()).isZero();
        }
    }
}
