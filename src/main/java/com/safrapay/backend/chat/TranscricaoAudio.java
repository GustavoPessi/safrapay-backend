package com.safrapay.backend.chat;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Transcrição de mensagens de voz (02/10/2026) pela API de áudio da OpenAI
 * (padrão: gpt-4o-mini-transcribe). Serviço separado do chat de propósito: o
 * webhook do WhatsApp vai reaproveitar o mesmo (baixa a mídia e chama
 * {@link #transcrever}).
 *
 * O áudio não é guardado (LGPD): vai para a API, volta o texto e os bytes são
 * descartados. No log ficam só formato, tamanho e quantidade de caracteres.
 *
 * A chave vem de OPENAI_API_KEY (só no .env da EC2). Sem chave, mensagens de
 * voz respondem 503 e o resto do chat funciona normalmente.
 */
@Component
public class TranscricaoAudio {

    private static final Logger log = LoggerFactory.getLogger(TranscricaoAudio.class);
    private static final String URL = "https://api.openai.com/v1/audio/transcriptions";

    /** Formato identificado pelos bytes do arquivo (extensão e tipo enviados à API). */
    public record Formato(String extensao, String mime) {}

    private final RestClient http;
    private final String chaveApi;
    private final String modelo;

    public TranscricaoAudio(RestClient.Builder builder,
                            @Value("${ia.transcricao.api-key:}") String chaveApi,
                            @Value("${ia.transcricao.modelo:gpt-4o-mini-transcribe}") String modelo) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(10_000);
        fabrica.setReadTimeout(60_000);
        this.http = builder.requestFactory(fabrica).build();
        this.chaveApi = chaveApi;
        this.modelo = modelo;
    }

    public boolean configurado() {
        return chaveApi != null && !chaveApi.isBlank();
    }

    /** Identifica o áudio pelos primeiros bytes, sem confiar no tipo que o app informa. null = não suportado. */
    public static Formato detectarFormato(byte[] b) {
        if (b == null || b.length < 12) return null;
        if (b[0] == 'O' && b[1] == 'g' && b[2] == 'g' && b[3] == 'S') return new Formato("ogg", "audio/ogg");
        if ((b[0] & 0xFF) == 0x1A && (b[1] & 0xFF) == 0x45 && (b[2] & 0xFF) == 0xDF && (b[3] & 0xFF) == 0xA3) {
            return new Formato("webm", "audio/webm");
        }
        if (b[4] == 'f' && b[5] == 't' && b[6] == 'y' && b[7] == 'p') return new Formato("m4a", "audio/mp4");
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'A' && b[10] == 'V' && b[11] == 'E') {
            return new Formato("wav", "audio/wav");
        }
        if ((b[0] == 'I' && b[1] == 'D' && b[2] == '3') || ((b[0] & 0xFF) == 0xFF && (b[1] & 0xE0) == 0xE0)) {
            return new Formato("mp3", "audio/mpeg");
        }
        return null;
    }

    /**
     * Transcreve o áudio em português.
     *
     * @param dica texto curto com o assunto e termos esperados (culturas, máquinas,
     *             categorias) — melhora "ureia", "glifosato", nomes de talhão
     * @return o texto transcrito (pode vir vazio se o áudio for só silêncio/ruído)
     */
    public String transcrever(byte[] audio, Formato formato, String dica) {
        HttpHeaders cabecalhoArquivo = new HttpHeaders();
        cabecalhoArquivo.setContentType(MediaType.parseMediaType(formato.mime()));
        ByteArrayResource arquivo = new ByteArrayResource(audio) {
            @Override
            public String getFilename() {
                return "audio." + formato.extensao();
            }
        };

        LinkedMultiValueMap<String, Object> corpo = new LinkedMultiValueMap<>();
        corpo.add("file", new HttpEntity<>(arquivo, cabecalhoArquivo));
        corpo.add("model", modelo);
        corpo.add("language", "pt");
        corpo.add("response_format", "json");
        if (dica != null && !dica.isBlank()) corpo.add("prompt", dica);

        JsonNode resposta = http.post()
                .uri(URL)
                .header("Authorization", "Bearer " + chaveApi)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(corpo)
                .retrieve()
                .body(JsonNode.class);

        String texto = resposta == null ? "" : resposta.path("text").asText("").trim();
        log.info("Audio transcrito: modelo={} formato={} bytes={} caracteres={}",
                modelo, formato.extensao(), audio.length, texto.length());
        return texto;
    }
}
