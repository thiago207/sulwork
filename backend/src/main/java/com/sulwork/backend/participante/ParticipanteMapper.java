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
        return new ParticipanteResposta(p.getId(), p.getNome(), mascararCpf(p.getCpf()), restricoes, p.getObservacaoRestricao());
    }

    // LGPD: a API nunca devolve o CPF inteiro (52998224725 -> ***.982.247-**)
    static String mascararCpf(String cpf) {
        return "***." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-**";
    }
}
