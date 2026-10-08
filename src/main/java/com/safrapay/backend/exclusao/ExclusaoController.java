package com.safrapay.backend.exclusao;

import com.safrapay.backend.exclusao.dto.ExclusaoSyncItem;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Exclusões sincronizadas (V6, 08/10/2026). Fica em /api/exclusoes/sync pra seguir o padrão
 * das outras rotas de sync (proteção da fazenda, colaborador barrado).
 */
@RestController
@RequestMapping("/api/exclusoes")
public class ExclusaoController {

    private final ExclusaoService service;

    public ExclusaoController(ExclusaoService service) {
        this.service = service;
    }

    @PostMapping("/sync")
    public Map<String, Integer> receber(@RequestBody List<ExclusaoSyncItem> itens) {
        return Map.of("processadas", service.receber(itens));
    }

    @GetMapping("/sync")
    public List<ExclusaoSyncItem> enviar(@RequestParam(required = false) String desde) {
        return service.enviar(desde);
    }
}
