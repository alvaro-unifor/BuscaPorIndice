package com.pbd.index.dtos.output;

import java.util.List;

public record PaginaDetalheDTO(
        int idPagina,
        List<String> registros
) {}

