package com.pbd.index.dtos.output;

public record BuscaChaveOutputDTO(
	String chave,
	int paginaId,
	long custoIndice,
	long custoTableScan,
	long tempoIndice,
	long tempoTableScan
) {}
