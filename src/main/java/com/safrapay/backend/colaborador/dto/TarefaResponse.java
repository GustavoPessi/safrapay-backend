package com.safrapay.backend.colaborador.dto;

import com.safrapay.backend.entity.AnotacaoMaquina;
import com.safrapay.backend.entity.Usuario;
import com.safrapay.backend.util.DataSyncUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Tarefa (anotação de máquina) na visão online do colaborador. */
public class TarefaResponse {

    private String uuid;
    private String maquinaUuid;
    private String maquinaNome;
    private String texto;
    /** yyyy-MM-dd ou null. */
    private String data;
    /** yyyy-MM-dd ou null. */
    private String prazo;
    private boolean concluida;
    /** Pendente com prazo vencido (calculado no fuso de Brasília). */
    private boolean atrasada;
    /** Atribuída a quem chamou. */
    private boolean minha;
    private Responsavel responsavel;
    private String criadoPorNome;
    private String concluidoEm;
    private String concluidoPorNome;
    /** Quando a tarefa foi criada/reportada (instante UTC ISO, com hora) — ex.: "Reportado em 08/10 às 14:32". */
    private String criadoEm;

    public static class Responsavel {
        private final String uuid;
        private final String nome;

        public Responsavel(String uuid, String nome) {
            this.uuid = uuid;
            this.nome = nome;
        }

        public String getUuid() { return uuid; }
        public String getNome() { return nome; }
    }

    public static TarefaResponse de(AnotacaoMaquina a, Usuario eu, LocalDate hoje) {
        TarefaResponse r = new TarefaResponse();
        r.uuid = a.getUuid();
        r.maquinaUuid = a.getMaquina().getUuid();
        r.maquinaNome = a.getMaquina().getNome();
        r.texto = a.getTexto();
        r.data = a.getData() == null ? null : a.getData().format(DateTimeFormatter.ISO_LOCAL_DATE);
        r.prazo = a.getPrazo() == null ? null : a.getPrazo().format(DateTimeFormatter.ISO_LOCAL_DATE);
        r.concluida = a.isConcluida();
        r.atrasada = a.isAtrasada(hoje);
        r.minha = a.getResponsavel() != null && a.getResponsavel().getId().equals(eu.getId());
        r.responsavel = a.getResponsavel() == null ? null
                : new Responsavel(a.getResponsavel().getUuid(), a.getResponsavel().getNome());
        r.criadoPorNome = a.getCriadoPor() == null ? null : a.getCriadoPor().getNome();
        r.concluidoEm = DataSyncUtil.formatar(a.getConcluidoEm());
        r.concluidoPorNome = a.getConcluidoPor() == null ? null : a.getConcluidoPor().getNome();
        r.criadoEm = DataSyncUtil.formatar(a.getCriadoEm());
        return r;
    }

    public String getUuid() { return uuid; }
    public String getMaquinaUuid() { return maquinaUuid; }
    public String getMaquinaNome() { return maquinaNome; }
    public String getTexto() { return texto; }
    public String getData() { return data; }
    public String getPrazo() { return prazo; }
    public boolean isConcluida() { return concluida; }
    public boolean isAtrasada() { return atrasada; }
    public boolean isMinha() { return minha; }
    public Responsavel getResponsavel() { return responsavel; }
    public String getCriadoPorNome() { return criadoPorNome; }
    public String getConcluidoEm() { return concluidoEm; }
    public String getConcluidoPorNome() { return concluidoPorNome; }
    public String getCriadoEm() { return criadoEm; }
}
