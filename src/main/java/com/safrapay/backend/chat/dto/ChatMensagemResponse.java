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

    /**
     * PROPOSTA = lançamentos completos, pedir confirmação; PRECISA_INFORMACAO = falta algo;
     * SEM_LANCAMENTO = conversa sem lançamento; CONSULTA = pergunta do produtor (o app calcula
     * o resultado no Room a partir de "consulta").
     */
    private String status;
    /** Texto pra mostrar na bolha da IA. */
    private String resposta;
    private List<LancamentoProposto> lancamentos = new ArrayList<>();
    /** Quantas mensagens o usuário ainda pode mandar hoje (limite diário de custo). */
    private Integer mensagensRestantesHoje;
    /** Avisos pra mostrar ao produtor (ex.: arquivo grande, só parte foi lida). Pode vir vazio. */
    private List<String> avisos = new ArrayList<>();
    /** Filtros da pergunta — só quando status = CONSULTA, senão null. */
    private Consulta consulta;
    /** Respostas rápidas (0 a 5, até 60 caracteres) que o app mostra como botões. */
    private List<String> sugestoes = new ArrayList<>();

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
    public Consulta getConsulta() { return consulta; }
    public void setConsulta(Consulta consulta) { this.consulta = consulta; }
    public List<String> getSugestoes() { return sugestoes; }
    public void setSugestoes(List<String> sugestoes) { this.sugestoes = sugestoes; }

    /**
     * Pergunta estruturada (contrato v2). A IA só monta os filtros; o app executa
     * no Room e mostra os números. Campos null = sem esse filtro.
     */
    public static class Consulta {
        /** GASTOS | RECEITAS | SALDO | LANCAMENTOS | SAFRAS | AREA | METAS | MAQUINAS */
        private String tipo;
        /** yyyy-MM-dd */
        private String dataInicio;
        /** yyyy-MM-dd */
        private String dataFim;
        private String safraUuid;
        private String propriedadeUuid;
        private String talhaoUuid;
        private String maquinaUuid;
        /** Nome exato de uma categoria da lista. */
        private String categoria;
        /** Ex.: "Soja" — todas as safras/talhões dessa cultura. */
        private String cultura;
        /** ATIVAS | ENCERRADAS | TODAS (só SAFRAS; null = ATIVAS). */
        private String situacao;
        /** CATEGORIA | SAFRA | MES | MAQUINA */
        private String agrupar;

        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
        public String getDataInicio() { return dataInicio; }
        public void setDataInicio(String dataInicio) { this.dataInicio = dataInicio; }
        public String getDataFim() { return dataFim; }
        public void setDataFim(String dataFim) { this.dataFim = dataFim; }
        public String getSafraUuid() { return safraUuid; }
        public void setSafraUuid(String safraUuid) { this.safraUuid = safraUuid; }
        public String getPropriedadeUuid() { return propriedadeUuid; }
        public void setPropriedadeUuid(String propriedadeUuid) { this.propriedadeUuid = propriedadeUuid; }
        public String getTalhaoUuid() { return talhaoUuid; }
        public void setTalhaoUuid(String talhaoUuid) { this.talhaoUuid = talhaoUuid; }
        public String getMaquinaUuid() { return maquinaUuid; }
        public void setMaquinaUuid(String maquinaUuid) { this.maquinaUuid = maquinaUuid; }
        public String getCategoria() { return categoria; }
        public void setCategoria(String categoria) { this.categoria = categoria; }
        public String getCultura() { return cultura; }
        public void setCultura(String cultura) { this.cultura = cultura; }
        public String getSituacao() { return situacao; }
        public void setSituacao(String situacao) { this.situacao = situacao; }
        public String getAgrupar() { return agrupar; }
        public void setAgrupar(String agrupar) { this.agrupar = agrupar; }
    }

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
