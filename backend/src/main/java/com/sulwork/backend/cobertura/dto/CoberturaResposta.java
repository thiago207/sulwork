package com.sulwork.backend.cobertura.dto;

import com.sulwork.backend.categoria.UnidadeMedida;
import java.math.BigDecimal;
import java.util.List;

public record CoberturaResposta(
        Long eventoId,
        BigDecimal coberturaGeral,
        int obrigatoriosCompletos,
        int obrigatoriosTotal,
        List<PorCategoria> categorias,
        List<PorItem> itens) {

    public record PorCategoria(Long categoriaId, String nome, UnidadeMedida unidade, long contribuido, int meta,
                               BigDecimal percentual) {
    }

    public record PorItem(Long itemId, String nome, Long categoriaId, UnidadeMedida unidade, boolean obrigatorio,
                          int necessario, long contribuido, long faltante, BigDecimal percentual) {
    }
}
