package com.sulwork.backend.participante.dto;

import com.sulwork.backend.participante.RestricaoAlimentar;
import com.sulwork.backend.participante.cpf.CPF;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record ParticipanteRequisicao(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String nome,

        @NotBlank(message = "O CPF é obrigatório.")
        @CPF
        String cpf,

        Set<RestricaoAlimentar> restricoes,

        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres.")
        String observacaoRestricao) {
}
