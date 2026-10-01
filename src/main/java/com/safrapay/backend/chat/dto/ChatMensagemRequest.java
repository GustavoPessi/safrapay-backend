package com.safrapay.backend.chat.dto;

import java.util.List;

/**
 * Corpo do POST /api/chat/mensagem (assistente de lançamentos com IA).
 *
 * O servidor não guarda a conversa: a cada chamada o app manda o histórico
 * recente ("mensagens", a última é a nova mensagem do produtor) e o
 * "contexto" do usuário (safras, categorias, máquinas com os uuids do Room),
 * pra IA saber, por exemplo, que "safra da soja" é a safra X — inclusive
 * coisas cadastradas offline que ainda não sincronizaram.
 */
public class ChatMensagemRequest {

    private List<Mensagem> mensagens;
    /** Data de hoje no aparelho (yyyy-MM-dd), pra resolver "ontem", "sexta passada"... */
    private String hoje;
    private Contexto contexto;

    public List<Mensagem> getMensagens() { return mensagens; }
    public void setMensagens(List<Mensagem> mensagens) { this.mensagens = mensagens; }
    public String getHoje() { return hoje; }
    public void setHoje(String hoje) { this.hoje = hoje; }
    public Contexto getContexto() { return contexto; }
    public void setContexto(Contexto contexto) { this.contexto = contexto; }

    public static class Mensagem {
        /** "usuario" ou "assistente". */
        private String papel;
        private String texto;

        public String getPapel() { return papel; }
        public void setPapel(String papel) { this.papel = papel; }
        public String getTexto() { return texto; }
        public void setTexto(String texto) { this.texto = texto; }
    }

    public static class Contexto {
        private List<Safra> safras;
        private List<String> categorias;
        private List<Maquina> maquinas;

        public List<Safra> getSafras() { return safras; }
        public void setSafras(List<Safra> safras) { this.safras = safras; }
        public List<String> getCategorias() { return categorias; }
        public void setCategorias(List<String> categorias) { this.categorias = categorias; }
        public List<Maquina> getMaquinas() { return maquinas; }
        public void setMaquinas(List<Maquina> maquinas) { this.maquinas = maquinas; }
    }

    public static class Safra {
        private String uuid;
        /** Ex.: "Soja • 2025/2026 • Talhão Ricardo" */
        private String descricao;
        /** true = safra sem data de fim (em andamento). */
        private Boolean ativa;

        public String getUuid() { return uuid; }
        public void setUuid(String uuid) { this.uuid = uuid; }
        public String getDescricao() { return descricao; }
        public void setDescricao(String descricao) { this.descricao = descricao; }
        public Boolean getAtiva() { return ativa; }
        public void setAtiva(Boolean ativa) { this.ativa = ativa; }
    }

    public static class Maquina {
        private String uuid;
        private String nome;

        public String getUuid() { return uuid; }
        public void setUuid(String uuid) { this.uuid = uuid; }
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
    }
}
