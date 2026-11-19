package com.sulwork.backend.evento.dto;

import com.sulwork.backend.evento.TipoEvento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record EventoRequisicao(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String nome,

        @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres.")
        String descricao,

        @NotNull(message = "O tipo é obrigatório.")
        TipoEvento tipo,

        @NotNull(message = "A data e hora é obrigatória.")
        LocalDateTime dataHora,

        @NotNull(message = "O prazo de alterações é obrigatório.")
        LocalDateTime prazoAlteracoes) {
}
