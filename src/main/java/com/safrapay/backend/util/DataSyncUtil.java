package com.safrapay.backend.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Formata/le instantes usados nos endpoints de sincronizacao (/sync).
 *
 * O app Android le esse valor com um SimpleDateFormat de padrao fixo
 * ("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"). Instant.toString() nao serve pra
 * gerar essa string porque varia a quantidade de digitos da fracao de
 * segundo (as vezes omite os milissegundos por completo) - por isso
 * formatamos sempre com 3 digitos fixos aqui. Pra ler o que o app manda,
 * Instant.parse aceita qualquer quantidade de digitos, entao nao precisa
 * de um parser dedicado.
 */
public final class DataSyncUtil {
    private DataSyncUtil() {}

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    public static String formatar(Instant instante) {
        return instante == null ? null : FORMATO.format(instante);
    }

    /**
     * Limite do pull a partir do marcador "desde" que o app manda (08/10/2026).
     *
     * O banco guarda atualizado_em com microssegundos, mas o app recebe (e devolve
     * como "desde") só milissegundos. Com "atualizadoEm > desde", o último item de
     * cada entidade (ex.: 12:12:37.399512 > 12:12:37.399) voltava em TODO pull, e o
     * app regravava esse item a cada sincronização — com a sincronização automática
     * isso virou um laço de pulls a cada poucos segundos. Agora o pull traz só o que
     * mudou a partir do milissegundo seguinte ao marcador: devolve desde + 999 µs
     * para usar com "...AtualizadoEmAfter(...)" (equivale a ">= desde + 1 ms").
     */
    public static Instant depoisDoMarcador(String desde) {
        return Instant.parse(desde).plusNanos(999_000);
    }
}
