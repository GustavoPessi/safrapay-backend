package com.safrapay.backend.propriedade.dto;

import java.math.BigDecimal;

/**
 * Item enviado pelo app no push de sincronizacao (POST /api/propriedades/sync).
 * Carrega o uuid gerado no celular (identidade estavel entre app e servidor)
 * e o instante da ultima alteracao local, usado pro "last write wins".
 */
public class PropriedadeSyncRequest {

    private String uuid;
    private String nome;
    private String localidade;
    private Double areaTotal;
    private String tipoSolo;
    private String matricula;
    private String cib;
    private BigDecimal valor;
    private String observacoes;
    private String atualizadoEm;

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getLocalidade() { return localidade; }
    public void setLocalidade(String localidade) { this.localidade = localidade; }
    public Double getAreaTotal() { return areaTotal; }
    public void setAreaTotal(Double areaTotal) { this.areaTotal = areaTotal; }
    public String getTipoSolo() { return tipoSolo; }
    public void setTipoSolo(String tipoSolo) { this.tipoSolo = tipoSolo; }
    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }
    public String getCib() { return cib; }
    public void setCib(String cib) { this.cib = cib; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public String getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(String atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
