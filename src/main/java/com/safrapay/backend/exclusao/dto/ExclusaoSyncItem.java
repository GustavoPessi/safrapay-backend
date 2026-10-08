package com.safrapay.backend.exclusao.dto;

/**
 * Aviso de exclusão trocado entre app e servidor (POST/GET /api/exclusoes/sync).
 * entidade: propriedade, talhao, safra, receita, despesa, meta, maquina ou anotacao_maquina
 * (os mesmos nomes dos marcadores de sync do app).
 */
public class ExclusaoSyncItem {

    private String entidade;
    private String uuid;
    /** ISO UTC. No push é só informativo: o servidor grava a hora dele. */
    private String excluidoEm;

    public ExclusaoSyncItem() {}

    public ExclusaoSyncItem(String entidade, String uuid, String excluidoEm) {
        this.entidade = entidade;
        this.uuid = uuid;
        this.excluidoEm = excluidoEm;
    }

    public String getEntidade() { return entidade; }
    public void setEntidade(String entidade) { this.entidade = entidade; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getExcluidoEm() { return excluidoEm; }
    public void setExcluidoEm(String excluidoEm) { this.excluidoEm = excluidoEm; }
}
