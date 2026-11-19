package com.sulwork.backend.contribuicao;

import static org.assertj.core.api.Assertions.assertThat;

import com.sulwork.backend.categoria.Categoria;
import com.sulwork.backend.categoria.UnidadeMedida;
import com.sulwork.backend.item.Alergeno;
import com.sulwork.backend.item.Item;
import com.sulwork.backend.participacao.Participacao;
import com.sulwork.backend.participante.Participante;
import com.sulwork.backend.participante.RestricaoAlimentar;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ContribuicaoTest {

    static Contribuicao contribuicao(Set<RestricaoAlimentar> restricoes, Set<Alergeno> alergenos) {
        var participante = new Participante("Ana", "52998224725", restricoes, null);
        var item = new Item(new Categoria(null, "Salgados", 10, UnidadeMedida.UNIDADE), "Torta", 5, false, alergenos);
        return new Contribuicao(new Participacao(null, participante), item, 1);
    }

    @Test
    void semConflitoNaoGeraAviso() {
        var c = contribuicao(Set.of(RestricaoAlimentar.SEM_GLUTEN), Set.of(Alergeno.LACTOSE));
        assertThat(c.avisosDeAlergeno()).isEmpty();
    }

    @Test
    void semRestricaoNaoGeraAviso() {
        var c = contribuicao(Set.of(), Set.of(Alergeno.GLUTEN, Alergeno.AMENDOIM));
        assertThat(c.avisosDeAlergeno()).isEmpty();
    }

    @Test
    void geraUmAvisoPorRestricaoComOsAlergenosConflitantes() {
        var c = contribuicao(Set.of(RestricaoAlimentar.SEM_GLUTEN, RestricaoAlimentar.VEGANO),
                Set.of(Alergeno.GLUTEN, Alergeno.OVO, Alergeno.LACTOSE, Alergeno.AMENDOIM));

        assertThat(c.avisosDeAlergeno()).containsExactly(
                "O item Torta contém LACTOSE, OVO, que conflita com sua restrição VEGANO.",
                "O item Torta contém GLUTEN, que conflita com sua restrição SEM_GLUTEN.");
    }
}
