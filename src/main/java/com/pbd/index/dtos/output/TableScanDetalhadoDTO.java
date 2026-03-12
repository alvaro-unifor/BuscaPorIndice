package com.pbd.index.dtos.output;

import java.util.List;

public record TableScanDetalhadoDTO(
        int paginaEncontrada,
        int custoPaginas,
        List<PagePreviewDTO> previews
) {}