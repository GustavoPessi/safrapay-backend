package com.safrapay.backend.chat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite de mensagens por usuário por dia no assistente de IA — proteção de
 * custo (cada mensagem é uma chamada paga à API). Guardado em memória: zera
 * à meia-noite (horário de Brasília) e também quando o serviço reinicia, o
 * que é aceitável pro volume atual. Se precisar de algo à prova de restart,
 * vira uma tabela via migration.
 */
@Component
public class LimiteDiarioIa {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final int limite;
    private final ConcurrentHashMap<Long, Uso> usos = new ConcurrentHashMap<>();

    private static final class Uso {
        final LocalDate dia;
        final int quantidade;
        Uso(LocalDate dia, int quantidade) { this.dia = dia; this.quantidade = quantidade; }
    }

    public LimiteDiarioIa(@Value("${ia.limite-diario:50}") int limite) {
        this.limite = limite;
    }

    /** Consome uma mensagem. Devolve quantas restam hoje, ou -1 se o limite já foi atingido. */
    public int consumir(Long usuarioId) {
        LocalDate hoje = LocalDate.now(FUSO);
        final int[] restante = {-1};
        usos.compute(usuarioId, (id, atual) -> {
            int usadas = (atual == null || !atual.dia.equals(hoje)) ? 0 : atual.quantidade;
            if (usadas >= limite) {
                restante[0] = -1;
                return atual;
            }
            restante[0] = limite - (usadas + 1);
            return new Uso(hoje, usadas + 1);
        });
        return restante[0];
    }

    /** Devolve a mensagem consumida (ex.: a chamada à IA falhou — não é justo contar). */
    public void devolver(Long usuarioId) {
        LocalDate hoje = LocalDate.now(FUSO);
        usos.computeIfPresent(usuarioId, (id, atual) ->
                atual.dia.equals(hoje) && atual.quantidade > 0 ? new Uso(hoje, atual.quantidade - 1) : atual);
    }
}
