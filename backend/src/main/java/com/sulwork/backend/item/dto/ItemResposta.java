package com.sulwork.backend.item.dto;

import com.sulwork.backend.categoria.UnidadeMedida;
import com.sulwork.backend.item.Alergeno;
import java.util.Set;

public record ItemResposta(
        Long id,
        String nome,
        Long categoriaId,
        String categoriaNome,
        UnidadeMedida unidade,
        int quantidadeNecessaria,
        boolean obrigatorio,
        Set<Alergeno> alergenos) {
}
