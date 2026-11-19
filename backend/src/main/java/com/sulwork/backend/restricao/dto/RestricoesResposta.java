package com.sulwork.backend.restricao.dto;

import com.sulwork.backend.item.Alergeno;
import com.sulwork.backend.participante.RestricaoAlimentar;
import java.util.List;
import java.util.Set;

public record RestricoesResposta(
        Long eventoId,
        int participantesAtivos,
        List<Resumo> resumo,
        List<PorItem> itens) {

    public record Resumo(RestricaoAlimentar restricao, long participantes) {
    }

    public record PorItem(Long itemId, String nome, Set<Alergeno> alergenos, long participantesAfetados,
                          Set<RestricaoAlimentar> restricoesEmConflito) {
    }
}
