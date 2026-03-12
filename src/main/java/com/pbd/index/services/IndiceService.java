package com.pbd.index.services;

import com.pbd.index.dtos.input.PageInputDTO;
import com.pbd.index.dtos.output.BuscaChaveOutputDTO;
import com.pbd.index.dtos.output.CarregarDadosOutputDTO;
import com.pbd.index.entities.Bucket;
import com.pbd.index.entities.EntradaIndice;
import com.pbd.index.entities.Pagina;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.pbd.index.dtos.output.IndiceStatusOutputDTO;
import com.pbd.index.dtos.output.PagePreviewDTO;
import com.pbd.index.dtos.output.BuscaIndiceDetalhadaDTO;
import com.pbd.index.dtos.output.TableScanDetalhadoDTO;
import com.pbd.index.dtos.output.BuscaChaveDetalhadaOutputDTO;
import com.pbd.index.dtos.output.BucketDetalheDTO;
import com.pbd.index.dtos.output.BucketEntradaDTO;
import com.pbd.index.dtos.output.PaginaDetalheDTO;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class IndiceService {

    private int tamanhoPagina;
    private List<Pagina> tabelaDeDados;
    private int nb;
    private final int fr = 20;
    private Bucket[] buckets;
    private long tempoConstrucaoIndice;
    private int nrTotal;
    private int qntdOverflow;
    private int qntdColisoes;

    public void criarPagina(PageInputDTO inputDTO) {
        setTamanhoPagina(inputDTO.tamanhoPagina());
    }

    public CarregarDadosOutputDTO processarCarga(MultipartFile arquivo) throws IOException {
        if (tamanhoPagina <= 0) {
            throw new IllegalStateException("Defina o tamanho da página antes de carregar. Use /indice/criar-pagina.");
        }

        int nr;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(arquivo.getInputStream(), StandardCharsets.UTF_8))) {

            nr = Math.toIntExact(reader.lines().count());
            int qntdPaginas = (int) Math.ceil((double) nr / tamanhoPagina);
            tabelaDeDados = new ArrayList<>(qntdPaginas);
        }

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
            }
        }

        this.nrTotal = nr;

        configurarECriarIndice(nr);

        return calcularEstatisticas(nr);
    }

    private void configurarECriarIndice(int nr) {
        long startTime = System.currentTimeMillis();
        int minimoNb = (nr > 0) ? (int) Math.ceil((double) nr / fr) + 1 : 2;
        this.nb = Math.max(2, minimoNb);

        this.buckets = new Bucket[nb];
        for (int i = 0; i < nb; i++) {
            buckets[i] = new Bucket(fr);
        }

        AtomicInteger qntdOverflow = new AtomicInteger(0);
        AtomicInteger qntdColisoes = new AtomicInteger(0);
        for (Pagina pagina : tabelaDeDados) {
            for (String chave : pagina.getRegistros()) {
                int enderecoBucket = funcaoHash(chave);
                buckets[enderecoBucket].adicionar(chave, pagina.getId(), qntdOverflow, qntdColisoes);
            }
        }

        this.qntdColisoes = qntdColisoes.get();
        this.qntdOverflow = qntdOverflow.get();

        this.tempoConstrucaoIndice = System.currentTimeMillis() - startTime;
    }

    private CarregarDadosOutputDTO calcularEstatisticas(int nr) {
        double taxaColisoes = (nr > 0) ? (double) qntdColisoes / nr * 100 : 0;
        double taxaOverflow = (nb > 0) ? (double) qntdOverflow / nb * 100 : 0;

        return new CarregarDadosOutputDTO(
                nr,
                qntdOverflow,
                qntdColisoes,
                taxaColisoes,
                taxaOverflow
        );
    }

    public BuscaChaveOutputDTO pesquisar(String chaveBusca) {
        long inicioIndice = System.nanoTime();
        BuscaIndiceDetalhadaDTO detalhe = buscaHashDetalhada(chaveBusca);
        int idPaginaEncontrada = detalhe.paginaId();
        long fimIndice = System.nanoTime();

        long inicioTableScan = System.nanoTime();
        int custoPaginasTableScan = buscaTableScan(chaveBusca);
        long fimTableScan = System.nanoTime();

        return new BuscaChaveOutputDTO(
                chaveBusca,
                idPaginaEncontrada,
                detalhe.custoIndice(),
                custoPaginasTableScan,
                (fimIndice - inicioIndice),
                (fimTableScan - inicioTableScan)
        );
    }

    private BuscaIndiceDetalhadaDTO buscaHashDetalhada(String chaveBusca) {
        int enderecoBucket = funcaoHash(chaveBusca);

        int bucketsPercorridos = 0;
        int paginaId = - 1;
        String chaveEncontrada = null;

        Bucket bucketAtual = buckets[enderecoBucket];

        while (bucketAtual != null) {
            bucketsPercorridos++;

            for (EntradaIndice entrada : bucketAtual.getEntradas()) {
                if (entrada.chave().equals(chaveBusca)) {
                    paginaId = entrada.paginaId();
                    break;
                }
            }

            if (paginaId != -1) break;
            bucketAtual = bucketAtual.getOverflow();
        }

        if (paginaId == -1) {
            return new BuscaIndiceDetalhadaDTO(
                    -1,
                    bucketsPercorridos,
                    enderecoBucket,
                    bucketsPercorridos,
                    null
            );
        }

        int custoIndice = 0;
        Pagina paginaEncontrada = tabelaDeDados.get(paginaId);
        custoIndice++;
        for (String registro : paginaEncontrada.getRegistros()) {
            if (registro.equals(chaveBusca)) {
                chaveEncontrada = registro;
                break;
            }
        }

        return new BuscaIndiceDetalhadaDTO(
                paginaId,
                custoIndice,
                enderecoBucket,
                bucketsPercorridos,
                chaveEncontrada
        );
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

    public TableScanDetalhadoDTO tableScanDetalhado(String chaveBusca) {
        return buscaTableScanDetalhada(chaveBusca);
    }

    private TableScanDetalhadoDTO buscaTableScanDetalhada(String chaveBusca) {
        int custoPaginas = 0;
        int paginaEncontrada = -1;

        List<PagePreviewDTO> previews = new ArrayList<>();

        for (Pagina p : tabelaDeDados) {
            custoPaginas++;

            previews.add(new PagePreviewDTO(
                    p.getId(),
                    p.getRegistros().stream().limit(5).toList()
            ));

            for (String registro : p.getRegistros()) {
                if (registro.equals(chaveBusca)) {
                    paginaEncontrada = p.getId();
                    return new TableScanDetalhadoDTO(
                            paginaEncontrada,
                            custoPaginas,
                            previews
                    );
                }
            }
        }

        return new TableScanDetalhadoDTO(
                -1,
                custoPaginas,
                previews
        );
    }

    public BuscaChaveDetalhadaOutputDTO pesquisarDetalhado(String chaveBusca) {
        long inicioIndice = System.nanoTime();
        BuscaIndiceDetalhadaDTO detalheIndice = buscaHashDetalhada(chaveBusca);
        long fimIndice = System.nanoTime();

        long inicioTableScan = System.nanoTime();
        TableScanDetalhadoDTO detalheScan = buscaTableScanDetalhada(chaveBusca);
        long fimTableScan = System.nanoTime();

        long tempoIndice = (fimIndice - inicioIndice);
        long tempoScan = (fimTableScan - inicioTableScan);

        return new BuscaChaveDetalhadaOutputDTO(
                chaveBusca,
                detalheIndice.paginaId(),
                detalheIndice.custoIndice(),
                tempoIndice,
                tempoScan,
                detalheIndice.enderecoBucket(),
                detalheIndice.bucketsPercorridos(),
                detalheScan,
                (tempoScan - tempoIndice)
        );
    }

    public IndiceStatusOutputDTO getStatus() {
        if (tabelaDeDados == null || tabelaDeDados.isEmpty() || buckets == null) {
            return null;
        }

        int totalPaginas = tabelaDeDados.size();

        Pagina primeira = tabelaDeDados.getFirst();
        Pagina ultima = tabelaDeDados.get(totalPaginas - 1);

        PagePreviewDTO primeiraPreview = new PagePreviewDTO(
                primeira.getId(),
                primeira.getRegistros().stream().limit(5).toList()
        );

        PagePreviewDTO ultimaPreview = new PagePreviewDTO(
                ultima.getId(),
                ultima.getRegistros().stream().limit(5).toList()
        );

        CarregarDadosOutputDTO est = calcularEstatisticas(nrTotal);

        return new IndiceStatusOutputDTO(
                nrTotal,
                tamanhoPagina,
                totalPaginas,
                fr,
                nb,
                tempoConstrucaoIndice,
                primeiraPreview,
                ultimaPreview,
                est.taxaColisoes(),
                est.taxaOverflow()
        );
    }

    public List<BucketDetalheDTO> listarBuckets() {
        if (buckets == null || nb == 0) {
            return new ArrayList<>();
        }

        List<BucketDetalheDTO> resultado = new ArrayList<>();

        for (int i = 0; i < nb; i++) {
            Bucket bucket = buckets[i];
            if (bucket == null) {
                continue;
            }

            List<BucketEntradaDTO> entradasDTO = new ArrayList<>();
            Bucket atual = bucket;
            int nivel = 0;

            while (atual != null) {
                for (EntradaIndice entrada : atual.getEntradas()) {
                    entradasDTO.add(new BucketEntradaDTO(
                            entrada.chave(),
                            entrada.paginaId(),
                            nivel
                    ));
                }
                atual = atual.getOverflow();
                nivel++;
            }

            resultado.add(new BucketDetalheDTO(i, entradasDTO));
        }

        return resultado;
    }

    public List<PaginaDetalheDTO> listarPaginas() {
        if (tabelaDeDados == null || tabelaDeDados.isEmpty()) {
            return new ArrayList<>();
        }

        List<PaginaDetalheDTO> paginas = new ArrayList<>();
        for (Pagina p : tabelaDeDados) {
            paginas.add(new PaginaDetalheDTO(
                    p.getId(),
                    p.getRegistros().stream().limit(5).toList()
            ));
        }
        return paginas;
    }

    private int funcaoHash(String chave) {
        long hash = 5381;

        for (int i = 0; i < chave.length(); i++) {
            hash = ((hash << 5) + hash) + chave.charAt(i);
        }

        return (int) (Math.abs(hash) % nb);
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

    public int getQntdOverflow() {
        return qntdOverflow;
    }

    public void setQntdOverflow(int qntdOverflow) {
        this.qntdOverflow = qntdOverflow;
    }

    public int getQntdColisoes() {
        return qntdColisoes;
    }

    public void setQntdColisoes(int qntdColisoes) {
        this.qntdColisoes = qntdColisoes;
    }
}


