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
import com.pbd.index.dtos.output.BucketDetalheDTO;
import com.pbd.index.dtos.output.PaginaDetalheDTO;
import com.pbd.index.dtos.output.TableScanDetalhadoDTO;

import java.util.List;

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
            CarregarDadosOutputDTO estatisticas = service.processarCarga(arquivo);

            return ResponseEntity.ok(estatisticas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/status")
    public ResponseEntity<IndiceStatusOutputDTO> status() {
        IndiceStatusOutputDTO status = service.getStatus();
        if (status == null) {
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

    @GetMapping("/table-scan")
    public ResponseEntity<TableScanDetalhadoDTO> tableScan(@RequestParam String chave) {
        if (service.getTabelaDeDados() == null || service.getTabelaDeDados().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(service.tableScanDetalhado(chave));
    }

    @GetMapping("/buckets")
    public ResponseEntity<List<BucketDetalheDTO>> listarBuckets() {
        if (service.getBuckets() == null || service.getNb() == 0) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(service.listarBuckets());
    }

    @GetMapping("/paginas")
    public ResponseEntity<List<PaginaDetalheDTO>> listarPaginas() {
        if (service.getTabelaDeDados() == null || service.getTabelaDeDados().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(service.listarPaginas());
    }
}