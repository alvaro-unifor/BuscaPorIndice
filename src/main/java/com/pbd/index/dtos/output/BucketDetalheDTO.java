package com.pbd.index.dtos.output;

import java.util.List;

public record BucketDetalheDTO(
        int indiceBucket,
        List<BucketEntradaDTO> entradas
) {}

