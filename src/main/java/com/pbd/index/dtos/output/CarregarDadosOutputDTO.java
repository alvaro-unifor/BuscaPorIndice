package com.pbd.index.dtos.output;

public record CarregarDadosOutputDTO (
    int quantidadeOverflow,
    int quantidadeColisoes,
    double taxaColisoes,
    double taxaOverflow
) {}
