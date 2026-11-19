package com.sulwork.backend.participante.dto;

import com.sulwork.backend.participante.RestricaoAlimentar;
import com.sulwork.backend.participante.cpf.CPF;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

// a API devolve o CPF mascarado, então na edição ele é opcional: sem CPF, mantém o atual
public record ParticipanteEdicaoRequisicao(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String nome,

        @CPF
        String cpf,

        Set<RestricaoAlimentar> restricoes,

        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres.")
        String observacaoRestricao) {
}
