package com.safrapay.backend.equipe.dto;

import com.safrapay.backend.entity.EquipeMembro;
import com.safrapay.backend.util.DataSyncUtil;

/** Convite pendente para quem está logado (tela 34 — Convite recebido). */
public class ConviteRecebidoResponse {

    private String uuid;
    private String donoNome;
    private String papel;
    private String criadoEm;

    public static ConviteRecebidoResponse de(EquipeMembro e) {
        ConviteRecebidoResponse r = new ConviteRecebidoResponse();
        r.uuid = e.getUuid();
        r.donoNome = e.getDono().getNome();
        r.papel = e.getPapel().name();
        r.criadoEm = DataSyncUtil.formatar(e.getCriadoEm());
        return r;
    }

    public String getUuid() { return uuid; }
    public String getDonoNome() { return donoNome; }
    public String getPapel() { return papel; }
    public String getCriadoEm() { return criadoEm; }
}
