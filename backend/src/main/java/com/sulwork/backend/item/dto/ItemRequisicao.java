package com.sulwork.backend.item.dto;

import com.sulwork.backend.item.Alergeno;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record ItemRequisicao(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String nome,

        @NotNull(message = "A categoria é obrigatória.")
        Long categoriaId,

        @NotNull(message = "A quantidade necessária é obrigatória.")
        @Positive(message = "A quantidade necessária deve ser maior que zero.")
        Integer quantidadeNecessaria,

        // Boolean e não boolean: no Jackson 3 um primitivo ausente no JSON vira erro 400
        Boolean obrigatorio,

        Set<Alergeno> alergenos) {
}
