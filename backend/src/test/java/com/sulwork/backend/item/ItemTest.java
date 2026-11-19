package com.sulwork.backend.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sulwork.backend.categoria.Categoria;
import com.sulwork.backend.categoria.UnidadeMedida;
import com.sulwork.backend.comum.erro.RegraNegocioException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ItemTest {

    final Categoria bebidas = new Categoria(null, "Bebidas", 10, UnidadeMedida.LITRO);

    Item item() {
        return new Item(bebidas, "Suco", 6, true, Set.of());
    }

    @Test
    void herdaAUnidadeDaCategoria() {
        var item = item();
        assertThat(item.getUnidade()).isEqualTo(UnidadeMedida.LITRO);

        bebidas.atualizar("Bebidas", 10, UnidadeMedida.GARRAFA);
        assertThat(item.getUnidade()).isEqualTo(UnidadeMedida.GARRAFA);
    }

    @Test
    void naoReduzAbaixoDoContribuido() {
        var item = item();

        assertThatThrownBy(() -> item.atualizar(bebidas, "Suco", 3, true, Set.of(), 4))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A quantidade necessária não pode ser menor que o total já contribuído (4).");
        assertThat(item.getQuantidadeNecessaria()).isEqualTo(6);
    }

    @Test
    void permiteReduzirAteOContribuido() {
        var item = item();

        item.atualizar(bebidas, "Suco", 4, true, Set.of(), 4);

        assertThat(item.getQuantidadeNecessaria()).isEqualTo(4);
    }

    @Test
    void atualizarTrocaOsAlergenos() {
        var item = new Item(bebidas, "Vitamina", 2, false, Set.of(Alergeno.LACTOSE));

        item.atualizar(bebidas, "Vitamina", 2, false, Set.of(Alergeno.MEL), 0);

        assertThat(item.getAlergenos()).containsExactly(Alergeno.MEL);
    }
}
