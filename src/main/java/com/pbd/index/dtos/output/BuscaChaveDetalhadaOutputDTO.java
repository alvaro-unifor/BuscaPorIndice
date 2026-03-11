package com.pbd.index.dtos.output;

public record BuscaChaveDetalhadaOutputDTO(
        String chave,
        int paginaIdIndice,
        int custoIndice,
        long tempoIndice,
	long tempoScan,
        int enderecoBucket,
        int bucketsPercorridos,
        TableScanDetalhadoDTO tableScan,
        long diferencaTempo
) {}