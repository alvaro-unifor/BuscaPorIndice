package com.pbd.index.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Bucket {
    private List<EntradaIndice> entradas;
    private int fr;
    private Bucket overflow;


    public Bucket(int fr) {
	this.fr = fr;
	this.entradas = new ArrayList<>(fr);
	this.overflow = null;
    }

    public boolean temEspaco() {
	return entradas.size() < fr;
    }

    public void adicionar(String chave, int paginaId, AtomicInteger qntdOverflow, AtomicInteger qntdColisoes) {
        if (temEspaco()) {
            entradas.add(new EntradaIndice(chave, paginaId));
        } else {
	    qntdColisoes.incrementAndGet();
            if (this.overflow == null) {
		qntdOverflow.incrementAndGet();
                this.overflow = new Bucket(fr);
            }
            this.overflow.adicionar(chave, paginaId, qntdOverflow, qntdColisoes);
        }
    }

    public List<EntradaIndice> getEntradas() {
	return entradas;
    }

    public void setEntradas(List<EntradaIndice> entradas) {
	this.entradas = entradas;
    }

    public Bucket getOverflow() {
	return overflow;
    }

    public void setOverflow(Bucket overflow) {
	this.overflow = overflow;
    }

    public int getFr() {
	return fr;
    }

    public void setFr(int fr) {
	this.fr = fr;
    }
}