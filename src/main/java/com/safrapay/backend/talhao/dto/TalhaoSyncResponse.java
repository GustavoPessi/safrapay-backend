package com.safrapay.backend.talhao.dto;

import com.safrapay.backend.entity.Talhao;
import com.safrapay.backend.util.DataSyncUtil;

public class TalhaoSyncResponse {

    private String uuid;
    private String propriedadeUuid;
    private String nome;
    private Double areaHa;
    private String cultura;
    private Double latitude;
    private Double longitude;
    private String atualizadoEm;

    public static TalhaoSyncResponse de(Talhao t) {
        TalhaoSyncResponse r = new TalhaoSyncResponse();
        r.uuid = t.getUuid();
        r.propriedadeUuid = t.getPropriedade().getUuid();
        r.nome = t.getNome();
        r.areaHa = t.getAreaHa();
        r.cultura = t.getCultura();
        r.latitude = t.getLatitude();
        r.longitude = t.getLongitude();
        r.atualizadoEm = DataSyncUtil.formatar(t.getAtualizadoEm());
        return r;
    }

    public String getUuid() { return uuid; }
    public String getPropriedadeUuid() { return propriedadeUuid; }
    public String getNome() { return nome; }
    public Double getAreaHa() { return areaHa; }
    public String getCultura() { return cultura; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getAtualizadoEm() { return atualizadoEm; }
}
