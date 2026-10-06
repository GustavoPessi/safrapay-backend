package com.safrapay.backend.equipe.dto;

import com.safrapay.backend.entity.EquipeMembro;
import com.safrapay.backend.util.DataSyncUtil;

/** Um membro ou convite da equipe (tela 33 — Equipe). */
public class EquipeMembroResponse {

    private String uuid;
    private String email;
    /** Nome da conta (null se o convidado ainda não se cadastrou). */
    private String nome;
    private String papel;
    /** PENDENTE, ACEITO ou RECUSADO. */
    private String status;
    private String criadoEm;

    public static EquipeMembroResponse de(EquipeMembro e) {
        EquipeMembroResponse r = new EquipeMembroResponse();
        r.uuid = e.getUuid();
        r.email = e.getEmail();
        r.nome = e.getMembro() == null ? null : e.getMembro().getNome();
        r.papel = e.getPapel().name();
        r.status = e.getStatus().name();
        r.criadoEm = DataSyncUtil.formatar(e.getCriadoEm());
        return r;
    }

    public String getUuid() { return uuid; }
    public String getEmail() { return email; }
    public String getNome() { return nome; }
    public String getPapel() { return papel; }
    public String getStatus() { return status; }
    public String getCriadoEm() { return criadoEm; }
}
