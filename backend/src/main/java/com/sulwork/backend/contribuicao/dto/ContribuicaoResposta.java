package com.sulwork.backend.contribuicao.dto;

import com.sulwork.backend.categoria.UnidadeMedida;
import java.util.List;

public record ContribuicaoResposta(
        Long id,
        Long eventoId,
        Long participanteId,
        String participanteNome,
        Long itemId,
        String itemNome,
        UnidadeMedida unidade,
        int quantidade,
        List<String> avisos) {
}
