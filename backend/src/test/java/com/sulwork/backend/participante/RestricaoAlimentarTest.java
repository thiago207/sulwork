package com.sulwork.backend.participante;

import static com.sulwork.backend.item.Alergeno.AMENDOIM;
import static com.sulwork.backend.item.Alergeno.CARNE;
import static com.sulwork.backend.item.Alergeno.FRUTOS_DO_MAR;
import static com.sulwork.backend.item.Alergeno.GLUTEN;
import static com.sulwork.backend.item.Alergeno.LACTOSE;
import static com.sulwork.backend.item.Alergeno.MEL;
import static com.sulwork.backend.item.Alergeno.OVO;
import static com.sulwork.backend.item.Alergeno.PEIXE;
import static org.assertj.core.api.Assertions.assertThat;

import com.sulwork.backend.item.Alergeno;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class RestricaoAlimentarTest {

    // escrito a partir da regra 11, sem reaproveitar o enum
    static final Map<RestricaoAlimentar, Set<Alergeno>> ESPERADO = Map.of(
            RestricaoAlimentar.VEGANO, Set.of(LACTOSE, OVO, CARNE, PEIXE, FRUTOS_DO_MAR, MEL),
            RestricaoAlimentar.VEGETARIANO, Set.of(CARNE, PEIXE, FRUTOS_DO_MAR),
            RestricaoAlimentar.SEM_GLUTEN, Set.of(GLUTEN),
            RestricaoAlimentar.SEM_LACTOSE, Set.of(LACTOSE),
            RestricaoAlimentar.ALERGIA_AMENDOIM, Set.of(AMENDOIM),
            RestricaoAlimentar.ALERGIA_FRUTOS_DO_MAR, Set.of(FRUTOS_DO_MAR),
            RestricaoAlimentar.ALERGIA_OVO, Set.of(OVO),
            RestricaoAlimentar.ALERGIA_PEIXE, Set.of(PEIXE),
            RestricaoAlimentar.SEM_MEL, Set.of(MEL));

    @ParameterizedTest
    @EnumSource(RestricaoAlimentar.class)
    void cadaRestricaoTemOsAlergenosDaRegra11(RestricaoAlimentar restricao) {
        assertThat(restricao.getAlergenos()).containsExactlyInAnyOrderElementsOf(ESPERADO.get(restricao));
    }

    @Test
    void veganoConflitaComItemQueTemLeite() {
        assertThat(RestricaoAlimentar.VEGANO.conflitaCom(List.of(GLUTEN, LACTOSE))).isTrue();
    }

    @Test
    void semGlutenNaoConflitaComItemSemGluten() {
        assertThat(RestricaoAlimentar.SEM_GLUTEN.conflitaCom(List.of(LACTOSE, OVO))).isFalse();
    }

    @Test
    void vegetarianoNaoConflitaComOvoNemLeite() {
        assertThat(RestricaoAlimentar.VEGETARIANO.conflitaCom(List.of(OVO, LACTOSE, MEL))).isFalse();
    }

    @ParameterizedTest
    @EnumSource(RestricaoAlimentar.class)
    void itemSemAlergenosNaoConflitaComNenhumaRestricao(RestricaoAlimentar restricao) {
        assertThat(restricao.conflitaCom(List.of())).isFalse();
    }
}
