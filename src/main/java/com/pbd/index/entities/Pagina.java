package com.pbd.index.entities;

import java.util.ArrayList;
import java.util.List;

public class Pagina {
    private int id;
    private List<String> registros;
    private int capacidade;

    public Pagina(int id, int capacidade) {
	this.id = id;
	this.capacidade = capacidade;
	this.registros = new ArrayList<>(capacidade);
    }

    public boolean estaCheia() {
	return registros.size() >= capacidade;
    }

    public void adicionarRegistro(String palavra) {
	if (!estaCheia()) {
	    registros.add(palavra);
	}
    }

    public int getId() {
	return id;
    }

    public void setId(int id) {
	this.id = id;
    }

    public List<String> getRegistros() {
	return registros;
    }

    public void setRegistros(List<String> registros) {
	this.registros = registros;
    }

    public int getCapacidade() {
	return capacidade;
    }

    public void setCapacidade(int capacidade) {
	this.capacidade = capacidade;
    }
}