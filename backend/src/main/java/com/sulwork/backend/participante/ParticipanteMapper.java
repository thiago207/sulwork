package com.sulwork.backend.participante;

import com.sulwork.backend.participante.dto.ParticipanteResposta;
import java.util.EnumSet;

public final class ParticipanteMapper {

    private ParticipanteMapper() {
    }

    public static ParticipanteResposta paraResposta(Participante p) {
        var restricoes = p.getRestricoes().isEmpty()
                ? EnumSet.noneOf(RestricaoAlimentar.class)
                : EnumSet.copyOf(p.getRestricoes());
        return new ParticipanteResposta(p.getId(), p.getNome(), p.getCpf(), restricoes, p.getObservacaoRestricao());
    }
}
