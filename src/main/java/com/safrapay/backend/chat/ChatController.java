package com.safrapay.backend.chat;

import com.safrapay.backend.chat.dto.ChatMensagemRequest;
import com.safrapay.backend.chat.dto.ChatMensagemResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chat com IA dentro do app (assistente de lançamentos). Autenticado (JWT),
 * como qualquer rota fora da lista pública do SecurityConfig.
 *
 * Códigos de resposta:
 *  200 - ok (ver "status" no corpo)
 *  400 - corpo inválido (sem mensagens / última mensagem não é do produtor)
 *  429 - limite diário de mensagens atingido
 *  502 - falha ao falar com o serviço de IA (tentar de novo)
 *  503 - assistente não configurado no servidor (sem ANTHROPIC_API_KEY)
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService service;

    public ChatController(ChatService service) {
        this.service = service;
    }

    @PostMapping("/mensagem")
    public ChatMensagemResponse mensagem(@RequestBody ChatMensagemRequest request) {
        return service.processar(request);
    }
}
