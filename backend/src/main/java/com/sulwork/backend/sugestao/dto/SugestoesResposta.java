package com.sulwork.backend.sugestao.dto;

import com.sulwork.backend.categoria.UnidadeMedida;
import com.sulwork.backend.participante.RestricaoAlimentar;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record SugestoesResposta(
        Long eventoId,
        Long participanteId,
        int ocultosPorConflito,
        List<Sugestao> sugestoes) {

    public record Sugestao(Long itemId, String item, String categoria, UnidadeMedida unidade, boolean obrigatorio,
                           long faltante, BigDecimal cobertura, int suaContribuicao, Conflitos conflitos) {
    }

    public record Conflitos(long participantes, Set<RestricaoAlimentar> restricoes, boolean comVoce) {
    }
}
