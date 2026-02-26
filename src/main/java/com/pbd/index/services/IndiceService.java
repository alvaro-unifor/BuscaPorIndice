package com.pbd.index.services;

import com.pbd.index.dtos.input.PageInputDTO;
import com.pbd.index.dtos.output.BuscaChaveOutputDTO;
import com.pbd.index.entities.Bucket;
import com.pbd.index.entities.EntradaIndice;
import com.pbd.index.entities.Pagina;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class IndiceService {

    private int tamanhoPagina;

    private List<Pagina> tabelaDeDados;

    private int nb;

    private final int fr = 4;

    private Bucket[] buckets;

    private long tempoConstrucaoIndice;

    public void criarPagina(PageInputDTO inputDTO) {
        setTamanhoPagina(inputDTO.tamanhoPagina());
    }

    public void processarCarga(MultipartFile arquivo) throws IOException {
        tabelaDeDados = new ArrayList<>();
        int nr = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(arquivo.getInputStream(), StandardCharsets.UTF_8))) {

            String linha;
            int contadorPagina = 0;
            Pagina paginaAtual = new Pagina(contadorPagina, tamanhoPagina);
            tabelaDeDados.add(paginaAtual);

            while ((linha = reader.readLine()) != null) {
                String palavra = linha.trim();
                if (palavra.isEmpty()) continue;

                if (paginaAtual.estaCheia()) {
                    contadorPagina++;
                    paginaAtual = new Pagina(contadorPagina, tamanhoPagina);
                    tabelaDeDados.add(paginaAtual);
                }

                paginaAtual.adicionarRegistro(palavra);
                nr++;
            }
        }

        configurarECriarIndice(nr);
    }

    private void configurarECriarIndice(int nr) {
        this.nb = (int) Math.ceil((double) nr / fr) + (nr / 100);

        this.buckets = new Bucket[nb];
        for (int i = 0; i < nb; i++) {
            buckets[i] = new Bucket(fr);
        }

        long startTime = System.currentTimeMillis();

        for (Pagina pagina : tabelaDeDados) {
            for (String chave : pagina.getRegistros()) {
                int enderecoBucket = funcaoHash(chave);
                buckets[enderecoBucket].adicionar(chave, pagina.getId());
            }
        }

        this.tempoConstrucaoIndice = System.currentTimeMillis() - startTime;
    }

    public BuscaChaveOutputDTO pesquisar(String chaveBusca) {
        // ÍNDICE HASH
        long inicioIndice = System.nanoTime();
        int idPaginaEncontrada = buscaHash(chaveBusca);
        long fimIndice = System.nanoTime();

        // FULL TABLE SCAN
        long inicioTableScan = System.nanoTime();
        int custoPaginasTableScan = buscaTableScan(chaveBusca);
        long fimTableScan = System.nanoTime();

        return new BuscaChaveOutputDTO(
                chaveBusca,
                idPaginaEncontrada,
                1,
                custoPaginasTableScan,
                (fimIndice - inicioIndice),
                (fimTableScan - inicioTableScan)
        );
    }

    private int buscaHash(String chaveBusca) {
        int enderecoBucket = funcaoHash(chaveBusca);
        int paginaId = - 1;

	Bucket bucketAtual = buckets[enderecoBucket];
        while (bucketAtual != null) {
            for (EntradaIndice entrada : bucketAtual.getEntradas()) {
                if (entrada.chave().equals(chaveBusca)) {
                    paginaId = entrada.paginaId();
                    break;
                }
            }
            bucketAtual = bucketAtual.getOverflow();
        }

        if (paginaId != -1) {
            Pagina pagina = tabelaDeDados.get(paginaId);

            for (String registro : pagina.getRegistros()) {
                if (registro.equals(chaveBusca)) {
                    break;
                }
            }
        }

        return paginaId;
    }

    private int buscaTableScan(String chaveBusca) {
        int custoPaginasTableScan = 0;

        for (Pagina p : tabelaDeDados) {
            custoPaginasTableScan++;
            for (String registro : p.getRegistros()) {
                if (registro.equals(chaveBusca)) {
                    return custoPaginasTableScan;
                }
            }
        }

        return -1;
    }

    private int funcaoHash(String chave) {
        return Math.abs(chave.hashCode()) % nb;
    }

    public int getTamanhoPagina() {
        return tamanhoPagina;
    }

    public void setTamanhoPagina(int tamanhoPagina) {
        this.tamanhoPagina = tamanhoPagina;
    }

    public List<Pagina> getTabelaDeDados() {
        return tabelaDeDados;
    }

    public void setTabelaDeDados(List<Pagina> tabelaDeDados) {
        this.tabelaDeDados = tabelaDeDados;
    }

    public int getNb() {
        return nb;
    }

    public void setNb(int nb) {
        this.nb = nb;
    }

    public Bucket[] getBuckets() {
        return buckets;
    }

    public void setBuckets(Bucket[] buckets) {
        this.buckets = buckets;
    }

    public long getTempoConstrucaoIndice() {
        return tempoConstrucaoIndice;
    }

    public void setTempoConstrucaoIndice(long tempoConstrucaoIndice) {
        this.tempoConstrucaoIndice = tempoConstrucaoIndice;
    }
}
