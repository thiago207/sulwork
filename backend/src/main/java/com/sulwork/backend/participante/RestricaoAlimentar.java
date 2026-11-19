package com.sulwork.backend.participante;

import static com.sulwork.backend.item.Alergeno.AMENDOIM;
import static com.sulwork.backend.item.Alergeno.CARNE;
import static com.sulwork.backend.item.Alergeno.FRUTOS_DO_MAR;
import static com.sulwork.backend.item.Alergeno.GLUTEN;
import static com.sulwork.backend.item.Alergeno.LACTOSE;
import static com.sulwork.backend.item.Alergeno.MEL;
import static com.sulwork.backend.item.Alergeno.OVO;
import static com.sulwork.backend.item.Alergeno.PEIXE;

import com.sulwork.backend.item.Alergeno;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

// regra 11: cada restrição declara os alérgenos com que conflita
public enum RestricaoAlimentar {
    VEGANO(LACTOSE, OVO, CARNE, PEIXE, FRUTOS_DO_MAR, MEL),
    VEGETARIANO(CARNE, PEIXE, FRUTOS_DO_MAR),
    SEM_GLUTEN(GLUTEN),
    SEM_LACTOSE(LACTOSE),
    ALERGIA_AMENDOIM(AMENDOIM),
    ALERGIA_FRUTOS_DO_MAR(FRUTOS_DO_MAR),
    ALERGIA_OVO(OVO);

    private final Set<Alergeno> alergenos;

    RestricaoAlimentar(Alergeno primeiro, Alergeno... outros) {
        this.alergenos = Collections.unmodifiableSet(EnumSet.of(primeiro, outros));
    }

    public Set<Alergeno> getAlergenos() {
        return alergenos;
    }

    public boolean conflitaCom(Collection<Alergeno> alergenosDoItem) {
        return alergenosDoItem.stream().anyMatch(alergenos::contains);
    }
}
