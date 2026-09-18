package com.safrapay.backend.propriedade.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public class PropriedadeRequest {

    @NotBlank(message = "Nome eh obrigatorio")
    private String nome;

    private String localidade;
    private Double areaTotal;
    private String tipoSolo;
    private String matricula;
    private String cib;
    private BigDecimal valor;
    private String observacoes;
    private Double latitude;
    private Double longitude;

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
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
