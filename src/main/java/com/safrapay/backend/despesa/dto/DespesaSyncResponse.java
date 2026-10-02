package com.safrapay.backend.despesa.dto;

import com.safrapay.backend.entity.Despesa;
import com.safrapay.backend.util.DataSyncUtil;

import java.time.format.DateTimeFormatter;

public class DespesaSyncResponse {

    private String uuid;
    private String safraUuid;
    private String categoriaNome;
    private String maquinaUuid;
    private String descricao;
    private Double valor;
    private String dataRegistro;
    private String atualizadoEm;

    public static DespesaSyncResponse de(Despesa d) {
        DespesaSyncResponse r = new DespesaSyncResponse();
        r.uuid = d.getUuid();
        r.safraUuid = d.getSafra().getUuid();
        r.categoriaNome = d.getCategoria().getNome();
        r.maquinaUuid = d.getMaquina() == null ? null : d.getMaquina().getUuid();
        r.descricao = d.getDescricao();
        r.valor = d.getValor() == null ? null : d.getValor().doubleValue();
        r.dataRegistro = d.getDataRegistro() == null ? null : d.getDataRegistro().format(DateTimeFormatter.ISO_LOCAL_DATE);
        r.atualizadoEm = DataSyncUtil.formatar(d.getAtualizadoEm());
        return r;
    }

    public String getUuid() { return uuid; }
    public String getSafraUuid() { return safraUuid; }
    public String getCategoriaNome() { return categoriaNome; }
    public String getMaquinaUuid() { return maquinaUuid; }
    public String getDescricao() { return descricao; }
    public Double getValor() { return valor; }
    public String getDataRegistro() { return dataRegistro; }
    public String getAtualizadoEm() { return atualizadoEm; }
}
