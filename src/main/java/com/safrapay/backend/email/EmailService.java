package com.safrapay.backend.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;

/**
 * Envio de e-mails transacionais via Amazon SES.
 *
 * IMPORTANTE (conta SES em sandbox): enquanto a conta AWS nao tiver "production
 * access" liberado no SES, so e possivel enviar e-mail para enderecos que
 * tambem foram verificados manualmente no console do SES (alem do remetente).
 * Ou seja: em testes, cadastre-se usando um e-mail que voce verificou no SES,
 * senao o envio falha com MessageRejected.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final SesClient sesClient;
    private final String remetente;
    private final String baseUrl;

    public EmailService(SesClient sesClient,
                         @Value("${aws.ses.remetente}") String remetente,
                         @Value("${app.base-url}") String baseUrl) {
        this.sesClient = sesClient;
        this.remetente = remetente;
        this.baseUrl = baseUrl;
    }

    public void enviarConfirmacaoEmail(String destinatario, String nome, String token) {
        String link = baseUrl + "/api/auth/confirmar-email?token=" + token;
        String corpoHtml = "<p>Ola, " + nome + "!</p>"
                + "<p>Obrigado por se cadastrar no SafraPay. Clique no link abaixo para confirmar seu e-mail:</p>"
                + "<p><a href=\"" + link + "\">" + link + "</a></p>"
                + "<p>Se voce nao fez esse cadastro, pode ignorar esta mensagem.</p>";
        String corpoTexto = "Ola, " + nome + "!\n\n"
                + "Obrigado por se cadastrar no SafraPay. Acesse o link abaixo para confirmar seu e-mail:\n"
                + link + "\n\n"
                + "Se voce nao fez esse cadastro, pode ignorar esta mensagem.";
        enviar(destinatario, "Confirme seu cadastro no SafraPay", corpoHtml, corpoTexto);
    }

    public void enviarRedefinicaoSenha(String destinatario, String nome, String token) {
        // Link pra uma pagina web publica servida pelo proprio backend
        // (src/main/resources/static/redefinir-senha.html), que recebe o
        // token e chama /api/auth/redefinir-senha. Escolhido no lugar de um
        // deep link pro app porque funciona em qualquer dispositivo/cliente
        // de e-mail, sem depender do app estar instalado onde a pessoa abriu
        // a mensagem. Quando houver dominio + HTTPS, essa mesma URL vira a
        // base de um Android App Link (abre o app se instalado, cai nessa
        // pagina como fallback caso contrario).
        String link = baseUrl + "/redefinir-senha.html?token=" + token;
        String corpoHtml = "<p>Ola, " + nome + "!</p>"
                + "<p>Recebemos um pedido para redefinir sua senha no SafraPay. "
                + "Clique no link abaixo para escolher uma nova senha (valido por 1 hora):</p>"
                + "<p><a href=\"" + link + "\">" + link + "</a></p>"
                + "<p>Se voce nao pediu isso, pode ignorar esta mensagem - sua senha atual continua valendo.</p>";
        String corpoTexto = "Ola, " + nome + "!\n\n"
                + "Recebemos um pedido para redefinir sua senha no SafraPay. "
                + "Acesse o link abaixo para escolher uma nova senha (valido por 1 hora):\n\n"
                + link + "\n\n"
                + "Se voce nao pediu isso, pode ignorar esta mensagem - sua senha atual continua valendo.";
        enviar(destinatario, "Redefinicao de senha - SafraPay", corpoHtml, corpoTexto);
    }

    private void enviar(String destinatario, String assunto, String corpoHtml, String corpoTexto) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(remetente)
                    .destination(Destination.builder().toAddresses(destinatario).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(assunto).charset("UTF-8").build())
                            .body(Body.builder()
                                    // Mandar tambem uma versao em texto simples (alem do HTML) ajuda
                                    // na pontuacao anti-spam dos provedores de e-mail.
                                    .html(Content.builder().data(corpoHtml).charset("UTF-8").build())
                                    .text(Content.builder().data(corpoTexto).charset("UTF-8").build())
                                    .build())
                            .build())
                    .build();
            sesClient.sendEmail(request);
        } catch (SesException e) {
            // Nao deixamos falha de e-mail derrubar o fluxo de cadastro/reset -
            // so registramos no log para investigar depois.
            log.error("Falha ao enviar e-mail via SES para {}: {}", destinatario, e.awsErrorDetails() != null
                    ? e.awsErrorDetails().errorMessage() : e.getMessage());
        }
    }
}
