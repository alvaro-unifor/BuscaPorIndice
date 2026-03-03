package com.pbd.index.dtos.output;

public record IndiceStatusOutputDTO(
        int nrTotalPalavras,
        int tamanhoPagina,
        int totalPaginas,
        int fr,
        int nb,
        long tempoConstrucaoIndiceMs,
        PagePreviewDTO primeiraPagina,
        PagePreviewDTO ultimaPagina,
        double taxaColisoes,
        double taxaOverflow
) {}