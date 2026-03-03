package com.pbd.index.controllers;

import com.pbd.index.dtos.input.PageInputDTO;
import com.pbd.index.dtos.output.BuscaChaveOutputDTO;
import com.pbd.index.dtos.output.CarregarDadosOutputDTO;
import com.pbd.index.services.IndiceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.pbd.index.dtos.output.IndiceStatusOutputDTO;
import com.pbd.index.dtos.output.BuscaChaveDetalhadaOutputDTO;

@RestController
@RequestMapping("/indice")
public class IndiceController {

    @Autowired
    private IndiceService service;

    @PostMapping("/criar-pagina")
    public void criarPagina(@RequestBody @Valid PageInputDTO inputDTO) {
        service.criarPagina(inputDTO);
    }

    @PostMapping("/carregar")
    public ResponseEntity<CarregarDadosOutputDTO> carregarArquivo(
            @RequestParam("arquivo") MultipartFile arquivo) {

        if (arquivo.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            // Agora o service devolve as estatísticas calculadas
            CarregarDadosOutputDTO estatisticas = service.processarCarga(arquivo);

            // Retornamos as estatísticas no corpo da resposta com status 200 (OK)
            return ResponseEntity.ok(estatisticas);
        } catch (Exception e) {
            // Em caso de erro, retornamos o status 500
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/status")
    public ResponseEntity<IndiceStatusOutputDTO> status() {
        IndiceStatusOutputDTO status = service.getStatus();
        if (status == null) {
            // ainda não carregou dados/índice
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(status);
    }

    @GetMapping("/pesquisar-detalhado")
    public ResponseEntity<BuscaChaveDetalhadaOutputDTO> pesquisarDetalhado(@RequestParam String chave) {
        return ResponseEntity.ok(service.pesquisarDetalhado(chave));
    }

    @GetMapping("/pesquisar")
    public BuscaChaveOutputDTO pesquisar(@RequestParam String chave) {
        return service.pesquisar(chave);
    }
}