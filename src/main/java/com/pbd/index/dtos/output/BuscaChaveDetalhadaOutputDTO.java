package com.pbd.index.dtos.output;

public record BuscaChaveDetalhadaOutputDTO(
        String chave,
        int paginaIdIndice,
        int custoIndice,
        long tempoIndice,
        TableScanDetalhadoDTO tableScan,
        long diferencaTempo // tableScan - indice (ns)
) {}