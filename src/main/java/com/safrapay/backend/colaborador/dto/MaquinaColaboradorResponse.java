package com.safrapay.backend.colaborador.dto;

import com.safrapay.backend.entity.Maquina;

/** GET /api/colaborador/maquinas — máquina com contadores das tarefas que quem chamou pode ver. */
public class MaquinaColaboradorResponse {

    private String uuid;
    private String nome;
    private String marca;
    private String modelo;
    private String tipo;
    private int tarefasPendentes;
    private int tarefasAtrasadas;

    public static MaquinaColaboradorResponse de(Maquina m, int pendentes, int atrasadas) {
        MaquinaColaboradorResponse r = new MaquinaColaboradorResponse();
        r.uuid = m.getUuid();
        r.nome = m.getNome();
        r.marca = m.getMarca();
        r.modelo = m.getModelo();
        r.tipo = m.getTipo();
        r.tarefasPendentes = pendentes;
        r.tarefasAtrasadas = atrasadas;
        return r;
    }

    public String getUuid() { return uuid; }
    public String getNome() { return nome; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public String getTipo() { return tipo; }
    public int getTarefasPendentes() { return tarefasPendentes; }
    public int getTarefasAtrasadas() { return tarefasAtrasadas; }
}
