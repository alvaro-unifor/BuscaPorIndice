package com.pbd.index.dtos.input;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PageInputDTO(
        @NotNull(message = "Tamanho da página é obrigatório")
        @Min(value = 1, message = "Tamanho da página deve ser maior que zero")
        int tamanhoPagina
) {}
