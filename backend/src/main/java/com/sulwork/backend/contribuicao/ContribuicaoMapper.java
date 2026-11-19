package com.sulwork.backend.contribuicao;

import com.sulwork.backend.contribuicao.dto.ContribuicaoResposta;

public final class ContribuicaoMapper {

    private ContribuicaoMapper() {
    }

    public static ContribuicaoResposta paraResposta(Contribuicao c) {
        var participacao = c.getParticipacao();
        var item = c.getItem();
        return new ContribuicaoResposta(c.getId(), participacao.getEvento().getId(),
                participacao.getParticipante().getId(), participacao.getParticipante().getNome(),
                item.getId(), item.getNome(), item.getUnidade(), c.getQuantidade(), c.avisosDeAlergeno());
    }
}
