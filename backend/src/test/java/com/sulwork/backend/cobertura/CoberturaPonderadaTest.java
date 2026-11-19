package com.sulwork.backend.cobertura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.sulwork.backend.cobertura.CalculoCobertura.Categoria;
import com.sulwork.backend.cobertura.CalculoCobertura.Item;
import java.util.List;
import org.junit.jupiter.api.Test;

class CoberturaPonderadaTest {

    final CalculoCobertura calculo = new CoberturaPonderada();

    static Item obrigatorio(int necessario, long contribuido) {
        return new Item(necessario, contribuido, true);
    }

    static Item opcional(int necessario, long contribuido) {
        return new Item(necessario, contribuido, false);
    }

    @Test
    void eventoVazioTemCoberturaZero() {
        assertThat(calculo.geral(List.of())).isZero();
    }

    @Test
    void tudoCompletoDa100() {
        var itens = List.of(obrigatorio(20, 20), obrigatorio(3, 3), opcional(4, 4));

        assertThat(calculo.geral(itens)).isEqualTo(1.0);
    }

    @Test
    void soOpcionaisCompletosPesamMenos() {
        // (2×0 + 2×0 + 1×1 + 1×1) / (2 + 2 + 1 + 1) = 2/6
        var itens = List.of(obrigatorio(10, 0), obrigatorio(5, 0), opcional(4, 4), opcional(2, 2));

        assertThat(calculo.geral(itens)).isCloseTo(1.0 / 3, within(1e-9));
    }

    @Test
    void soObrigatoriosCompletosPesamMais() {
        // (2×1 + 1×0) / 3 = 2/3
        var itens = List.of(obrigatorio(10, 10), opcional(4, 0));

        assertThat(calculo.geral(itens)).isCloseTo(2.0 / 3, within(1e-9));
    }

    @Test
    void exemploNumericoDaRegra10() {
        var pao = obrigatorio(20, 15);
        var bolo = opcional(2, 0);
        var cafe = obrigatorio(3, 3);
        var suco = opcional(4, 1);

        assertThat(calculo.porItem(pao)).isEqualTo(0.75);
        assertThat(calculo.porItem(suco)).isEqualTo(0.25);
        assertThat(calculo.geral(List.of(pao, bolo, cafe, suco))).isEqualTo(0.625);
        assertThat(calculo.porCategoria(new Categoria(25, 15))).isEqualTo(0.6);
        assertThat(calculo.porCategoria(new Categoria(6, 4))).isCloseTo(2.0 / 3, within(1e-9));
    }

    @Test
    void categoriaAcimaDaMetaFicaEm100() {
        assertThat(calculo.porCategoria(new Categoria(3, 4))).isEqualTo(1.0);
    }

    @Test
    void categoriaSemContribuicaoFicaEmZero() {
        assertThat(calculo.porCategoria(new Categoria(10, 0))).isZero();
    }

    @Test
    void itemNuncaPassaDe100() {
        assertThat(calculo.porItem(opcional(4, 6))).isEqualTo(1.0);
    }

    @Test
    void percentualArredondaParaUmaCasa() {
        assertThat(CoberturaService.percentual(0.625)).hasToString("62.5");
        assertThat(CoberturaService.percentual(2.0 / 3)).hasToString("66.7");
        assertThat(CoberturaService.percentual(0)).hasToString("0.0");
    }
}
