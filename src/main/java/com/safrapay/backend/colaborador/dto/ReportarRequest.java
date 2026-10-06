package com.safrapay.backend.colaborador.dto;

/** POST /api/colaborador/maquinas/{uuid}/tarefas — reportar problema numa máquina. */
public class ReportarRequest {

    private String texto;

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
}
