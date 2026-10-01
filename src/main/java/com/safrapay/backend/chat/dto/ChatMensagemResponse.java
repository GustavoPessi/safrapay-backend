package com.safrapay.backend.chat.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Resposta do POST /api/chat/mensagem. O backend NÃO grava nada: devolve a
 * proposta, o app mostra o card de confirmação e, no "sim", grava no Room
 * pelo mesmo caminho de uma despesa/receita cadastrada pela tela (e depois
 * sincroniza normalmente).
 */
public class ChatMensagemResponse {

    /** PROPOSTA = lançamentos completos, pedir confirmação; PRECISA_INFORMACAO = falta algo; SEM_LANCAMENTO = conversa sem lançamento. */
    private String status;
    /** Texto pra mostrar na bolha da IA. */
    private String resposta;
    private List<LancamentoProposto> lancamentos = new ArrayList<>();
    /** Quantas mensagens o usuário ainda pode mandar hoje (limite diário de custo). */
    private Integer mensagensRestantesHoje;
    /** Avisos pra mostrar ao produtor (ex.: arquivo grande, só parte foi lida). Pode vir vazio. */
    private List<String> avisos = new ArrayList<>();

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResposta() { return resposta; }
    public void setResposta(String resposta) { this.resposta = resposta; }
    public List<LancamentoProposto> getLancamentos() { return lancamentos; }
    public void setLancamentos(List<LancamentoProposto> lancamentos) { this.lancamentos = lancamentos; }
    public Integer getMensagensRestantesHoje() { return mensagensRestantesHoje; }
    public void setMensagensRestantesHoje(Integer mensagensRestantesHoje) { this.mensagensRestantesHoje = mensagensRestantesHoje; }
    public List<String> getAvisos() { return avisos; }
    public void setAvisos(List<String> avisos) { this.avisos = avisos; }

    public static class LancamentoProposto {
        /** "DESPESA" ou "RECEITA". */
        private String tipo;
        private Double valor;
        /** yyyy-MM-dd */
        private String data;
        private String descricao;
        /** Nome exato de uma categoria do contexto (só despesa). */
        private String categoria;
        /** uuid de uma safra do contexto. */
        private String safraUuid;
        /** uuid de uma máquina do contexto (opcional, só despesa). */
        private String maquinaUuid;
        /** Campos que faltam pra gravar: valor, data, descricao, categoria, safra. Vazio = completo. */
        private List<String> camposFaltando = new ArrayList<>();

        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
        public Double getValor() { return valor; }
        public void setValor(Double valor) { this.valor = valor; }
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
        public String getDescricao() { return descricao; }
        public void setDescricao(String descricao) { this.descricao = descricao; }
        public String getCategoria() { return categoria; }
        public void setCategoria(String categoria) { this.categoria = categoria; }
        public String getSafraUuid() { return safraUuid; }
        public void setSafraUuid(String safraUuid) { this.safraUuid = safraUuid; }
        public String getMaquinaUuid() { return maquinaUuid; }
        public void setMaquinaUuid(String maquinaUuid) { this.maquinaUuid = maquinaUuid; }
        public List<String> getCamposFaltando() { return camposFaltando; }
        public void setCamposFaltando(List<String> camposFaltando) { this.camposFaltando = camposFaltando; }
    }
}
