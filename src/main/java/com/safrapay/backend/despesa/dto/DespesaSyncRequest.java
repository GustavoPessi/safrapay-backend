package com.safrapay.backend.despesa.dto;

/**
 * Item enviado pelo app no push de sincronizacao (POST /api/despesas/sync).
 *
 * Relacionamentos viajam por identificadores estaveis, nunca por id:
 * - "safraUuid": safra-pai (obrigatoria).
 * - "categoriaNome": categoria de despesa pelo NOME (tabela fixa de 7 nomes,
 *   unica nos dois lados; os ids autoincremento nao coincidem).
 * - "maquinaUuid": maquina opcional. Desde a Fase 4 a maquina sincroniza
 *   antes da despesa; se o servidor ainda assim nao conhecer a maquina, o
 *   vinculo e ignorado aqui e o app preserva o vinculo local.
 */
public class DespesaSyncRequest {

    private String uuid;
    private String safraUuid;
    private String categoriaNome;
    private String maquinaUuid;
    private String descricao;
    private Double valor;
    private String dataRegistro;
    private String atualizadoEm;

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getSafraUuid() { return safraUuid; }
    public void setSafraUuid(String safraUuid) { this.safraUuid = safraUuid; }
    public String getCategoriaNome() { return categoriaNome; }
    public void setCategoriaNome(String categoriaNome) { this.categoriaNome = categoriaNome; }
    public String getMaquinaUuid() { return maquinaUuid; }
    public void setMaquinaUuid(String maquinaUuid) { this.maquinaUuid = maquinaUuid; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }
    public String getDataRegistro() { return dataRegistro; }
    public void setDataRegistro(String dataRegistro) { this.dataRegistro = dataRegistro; }
    public String getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(String atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
