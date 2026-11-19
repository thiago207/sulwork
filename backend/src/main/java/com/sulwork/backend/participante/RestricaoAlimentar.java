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
    VEGANO("vegano", LACTOSE, OVO, CARNE, PEIXE, FRUTOS_DO_MAR, MEL),
    VEGETARIANO("vegetariano", CARNE, PEIXE, FRUTOS_DO_MAR),
    SEM_GLUTEN("sem glúten", GLUTEN),
    SEM_LACTOSE("sem lactose", LACTOSE),
    SEM_MEL("sem mel", MEL),
    ALERGIA_AMENDOIM("alergia a amendoim", AMENDOIM),
    ALERGIA_FRUTOS_DO_MAR("alergia a frutos do mar", FRUTOS_DO_MAR),
    ALERGIA_OVO("alergia a ovo", OVO),
    ALERGIA_PEIXE("alergia a peixe", PEIXE);

    private final String rotulo;
    private final Set<Alergeno> alergenos;

    RestricaoAlimentar(String rotulo, Alergeno primeiro, Alergeno... outros) {
        this.rotulo = rotulo;
        this.alergenos = Collections.unmodifiableSet(EnumSet.of(primeiro, outros));
    }

    public String getRotulo() {
        return rotulo;
    }

    public Set<Alergeno> getAlergenos() {
        return alergenos;
    }

    public boolean conflitaCom(Collection<Alergeno> alergenosDoItem) {
        return alergenosDoItem.stream().anyMatch(alergenos::contains);
    }
}
