package com.pbd.index.dtos.output;

import java.util.List;

public record PagePreviewDTO(
        int idPagina,
        List<String> primeiros5Registros
) {}