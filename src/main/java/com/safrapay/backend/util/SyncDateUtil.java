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
public final class SyncDateUtil {
    private SyncDateUtil() {}

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    public static String formatar(Instant instant) {
        return instant == null ? null : FORMATTER.format(instant);
    }
}
