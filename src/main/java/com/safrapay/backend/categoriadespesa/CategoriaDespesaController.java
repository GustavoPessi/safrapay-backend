package com.safrapay.backend.categoriadespesa;

import com.safrapay.backend.categoriadespesa.dto.CategoriaDespesaResponse;
import com.safrapay.backend.repository.CategoriaDespesaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Categorias de despesa sao uma tabela de lookup fixa (seed), compartilhada
 * entre todos os usuarios - por isso so tem leitura, sem CRUD completo.
 */
@RestController
@RequestMapping("/api/categorias-despesa")
public class CategoriaDespesaController {

    private final CategoriaDespesaRepository repository;

    public CategoriaDespesaController(CategoriaDespesaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<CategoriaDespesaResponse> listar() {
        return repository.findAll().stream().map(CategoriaDespesaResponse::de).toList();
    }
}
