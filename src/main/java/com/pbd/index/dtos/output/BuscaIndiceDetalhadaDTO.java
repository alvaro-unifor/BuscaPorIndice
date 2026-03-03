package com.pbd.index.dtos.output;

public record BuscaIndiceDetalhadaDTO(
        int paginaId,
        int custoIndice,
        int enderecoBucket,
        int bucketsPercorridos
) {}