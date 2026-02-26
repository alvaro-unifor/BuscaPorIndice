package com.pbd.index.dtos.input;
import jakarta.validation.constraints.NotNull;

public record PageInputDTO (

	@NotNull
	int tamanhoPagina
){}
