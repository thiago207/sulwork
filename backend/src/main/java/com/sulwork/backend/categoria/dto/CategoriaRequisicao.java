package com.sulwork.backend.categoria.dto;

import com.sulwork.backend.categoria.UnidadeMedida;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CategoriaRequisicao(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 80, message = "O nome deve ter no máximo 80 caracteres.")
        String nome,

        @NotNull(message = "A meta é obrigatória.")
        @Positive(message = "A meta deve ser maior que zero.")
        Integer meta,

        @NotNull(message = "A unidade é obrigatória.")
        UnidadeMedida unidade) {
}
