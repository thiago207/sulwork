package com.sulwork.backend.participante.dto;

import com.sulwork.backend.participante.RestricaoAlimentar;
import java.util.Set;

public record ParticipanteResposta(
        Long id,
        String nome,
        String cpf,
        Set<RestricaoAlimentar> restricoes,
        String observacaoRestricao) {
}
