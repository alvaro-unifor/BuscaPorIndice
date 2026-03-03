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

@Service
public class IndiceService {

    private int tamanhoPagina;
    private List<Pagina> tabelaDeDados;
    private int nb;
    private final int fr = 4;
    private Bucket[] buckets;
    private long tempoConstrucaoIndice;
    private int nrTotal;

    public void criarPagina(PageInputDTO inputDTO) {
        setTamanhoPagina(inputDTO.tamanhoPagina());
    }

    // Alterado para retornar CarregarDadosOutputDTO
    public CarregarDadosOutputDTO processarCarga(MultipartFile arquivo) throws IOException {
        if (tamanhoPagina <= 0) {
            throw new IllegalStateException("Defina o tamanho da página antes de carregar. Use /indice/criar-pagina.");
        }

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

        this.nrTotal = nr;

        configurarECriarIndice(nr);

        // Retorna o cálculo de estatísticas após a construção do índice
        return calcularEstatisticas(nr);
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

    // Nova funcionalidade: Cálculo de estatísticas de colisão e overflow
    private CarregarDadosOutputDTO calcularEstatisticas(int nr) {
        int registrosColididos = 0;
        int bucketsComOverflow = 0;

        for (Bucket b : buckets) {
            // Identifica se o bucket original precisou de encadeamento
            if (b.getOverflow() != null) {
                bucketsComOverflow++;

                // Contabiliza registros que excederam o tamanho original do Bucket (FR)
                Bucket atual = b.getOverflow();
                while (atual != null) {
                    registrosColididos += atual.getEntradas().size();
                    atual = atual.getOverflow();
                }
            }
        }

        // Cálculo das taxas percentuais
        double taxaColisoes = (double) registrosColididos / nr * 100;
        double taxaOverflow = (double) bucketsComOverflow / nb * 100;

        return new CarregarDadosOutputDTO(
                bucketsComOverflow,
                registrosColididos,
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

        // Custo: 1 por bucket lido (principal + overflows)
        // Se achou, +1 por ler a página de dados
        int custoIndice = bucketsPercorridos;
        if (paginaId != -1) {
            custoIndice += 1;
        }

        return new BuscaIndiceDetalhadaDTO(
            paginaId,
            custoIndice,
            enderecoBucket,
            bucketsPercorridos
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

    private TableScanDetalhadoDTO buscaTableScanDetalhada(String chaveBusca) {
        int custoPaginas = 0;
        int paginaEncontrada = -1;

        List<Integer> paginasVisitadas = new ArrayList<>();
        List<PagePreviewDTO> previews = new ArrayList<>();

        for (Pagina p : tabelaDeDados) {
            custoPaginas++;
            paginasVisitadas.add(p.getId());

            // preview dos primeiros 5 registros da página (o que "foi lido")
            previews.add(new PagePreviewDTO(
                p.getId(),
                p.getRegistros().stream().limit(5).toList()
            ));

            // varredura completa da página
            for (String registro : p.getRegistros()) {
                if (registro.equals(chaveBusca)) {
                    paginaEncontrada = p.getId();
                    return new TableScanDetalhadoDTO(
                        paginaEncontrada,
                        custoPaginas,
                        paginasVisitadas,
                        previews
                    );
                }
            }
        }

        return new TableScanDetalhadoDTO(
            -1,
            custoPaginas, // leu tudo e não achou
            paginasVisitadas,
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
                detalheIndice.enderecoBucket(),
                detalheIndice.bucketsPercorridos(),
                detalheScan,
                (tempoScan - tempoIndice)
        );
    }

    public IndiceStatusOutputDTO getStatus() {
        if (tabelaDeDados == null || tabelaDeDados.isEmpty() || buckets == null) {
            // Você pode trocar por exceção depois, mas assim já funciona.
            return null;
        }

        int totalPaginas = tabelaDeDados.size();

        Pagina primeira = tabelaDeDados.get(0);
        Pagina ultima = tabelaDeDados.get(totalPaginas - 1);

        PagePreviewDTO primeiraPreview = new PagePreviewDTO(
                primeira.getId(),
                primeira.getRegistros().stream().limit(5).toList()
        );

        PagePreviewDTO ultimaPreview = new PagePreviewDTO(
                ultima.getId(),
                ultima.getRegistros().stream().limit(5).toList()
        );

        // Reusa a lógica que você já tem para taxas:
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
                    new ArrayList<>(p.getRegistros())
            ));
        }
        return paginas;
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


