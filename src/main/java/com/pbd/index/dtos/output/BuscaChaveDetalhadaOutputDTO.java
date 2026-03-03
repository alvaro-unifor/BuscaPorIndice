package com.pbd.index.dtos.output;

public record BuscaChaveDetalhadaOutputDTO(
        String chave,
        int paginaIdIndice,
        int custoIndice,
        long tempoIndice,
        int enderecoBucket,
        int bucketsPercorridos,
        TableScanDetalhadoDTO tableScan,
        long diferencaTempo // tableScan - indice (ns)
) {}