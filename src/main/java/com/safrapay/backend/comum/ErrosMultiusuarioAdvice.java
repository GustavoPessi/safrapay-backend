package com.safrapay.backend.comum;

import com.safrapay.backend.colaborador.ColaboradorController;
import com.safrapay.backend.equipe.EquipeController;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Erros das rotas de equipe e colaborador com corpo legível pelo app:
 * {"status": 409, "mensagem": "..."} — o corpo padrão do Spring Boot não traz a
 * mensagem. 403 de papel (ex.: gerente tentando gerenciar a equipe) leva o
 * cabeçalho X-SafraPay-Erro: PAPEL, igual ao SecurityConfig, pro app não
 * confundir com token vencido.
 */
@RestControllerAdvice(assignableTypes = {EquipeController.class, ColaboradorController.class})
public class ErrosMultiusuarioAdvice {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> statusException(ResponseStatusException e) {
        return corpo(e.getStatusCode(), e.getReason());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> corpoInvalido(HttpMessageNotReadableException e) {
        return corpo(HttpStatusCode.valueOf(400), "Corpo da requisição inválido.");
    }

    private ResponseEntity<Map<String, Object>> corpo(HttpStatusCode status, String mensagem) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("status", status.value());
        corpo.put("mensagem", mensagem);
        ResponseEntity.BodyBuilder resposta = ResponseEntity.status(status);
        if (status.value() == 403) resposta.header("X-SafraPay-Erro", "PAPEL");
        return resposta.body(corpo);
    }
}
