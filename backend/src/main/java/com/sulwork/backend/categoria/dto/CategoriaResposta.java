package com.sulwork.backend.categoria.dto;

import com.sulwork.backend.categoria.UnidadeMedida;

public record CategoriaResposta(Long id, Long eventoId, String nome, int meta, UnidadeMedida unidade) {
}
