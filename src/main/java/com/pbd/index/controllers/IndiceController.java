package com.pbd.index.controllers;

import com.pbd.index.dtos.input.PageInputDTO;
import com.pbd.index.dtos.output.BuscaChaveOutputDTO;
import com.pbd.index.services.IndiceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    public ResponseEntity<String> carregarArquivo(
            @RequestParam("arquivo") MultipartFile arquivo) {

        if (arquivo.isEmpty()) {
            return ResponseEntity.badRequest().body("Arquivo vazio.");
        }

        try {
            service.processarCarga(arquivo);
            return ResponseEntity.ok("Arquivo processado com sucesso!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro: " + e.getMessage());
        }
    }

    @GetMapping("/pesquisar")
    public BuscaChaveOutputDTO pesquisar(@RequestParam String chave) {
	return service.pesquisar(chave);
    }
}
