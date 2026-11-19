package com.sulwork.backend.item;

import com.sulwork.backend.item.dto.ItemResposta;
import java.util.EnumSet;

public final class ItemMapper {

    private ItemMapper() {
    }

    public static ItemResposta paraResposta(Item i) {
        var alergenos = EnumSet.noneOf(Alergeno.class);
        alergenos.addAll(i.getAlergenos());
        return new ItemResposta(i.getId(), i.getNome(), i.getCategoria().getId(), i.getCategoria().getNome(),
                i.getUnidade(), i.getQuantidadeNecessaria(), i.isObrigatorio(), alergenos);
    }
}
