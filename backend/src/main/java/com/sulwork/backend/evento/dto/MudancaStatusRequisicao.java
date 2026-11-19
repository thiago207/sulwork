package com.sulwork.backend.evento.dto;

import com.sulwork.backend.evento.StatusEvento;
import jakarta.validation.constraints.NotNull;

public record MudancaStatusRequisicao(@NotNull(message = "O status é obrigatório.") StatusEvento status) {
}
