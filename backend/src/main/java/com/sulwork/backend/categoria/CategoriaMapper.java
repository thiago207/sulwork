package com.sulwork.backend.categoria;

import com.sulwork.backend.categoria.dto.CategoriaResposta;

public final class CategoriaMapper {

    private CategoriaMapper() {
    }

    public static CategoriaResposta paraResposta(Categoria c) {
        return new CategoriaResposta(c.getId(), c.getEvento().getId(), c.getNome(), c.getMeta(), c.getUnidade());
    }
}
