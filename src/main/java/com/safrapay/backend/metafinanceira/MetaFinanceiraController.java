package com.safrapay.backend.metafinanceira;

import com.safrapay.backend.metafinanceira.dto.MetaFinanceiraRequest;
import com.safrapay.backend.metafinanceira.dto.MetaFinanceiraResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metas-financeiras")
public class MetaFinanceiraController {

    private final MetaFinanceiraService service;

    public MetaFinanceiraController(MetaFinanceiraService service) {
        this.service = service;
    }

    @GetMapping
    public List<MetaFinanceiraResponse> listar(@RequestParam Long culturaId) {
        return service.listarPorCultura(culturaId);
    }

    @GetMapping("/{id}")
    public MetaFinanceiraResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<MetaFinanceiraResponse> criar(@Valid @RequestBody MetaFinanceiraRequest request) {
        return ResponseEntity.ok(service.criar(request));
    }

    @PutMapping("/{id}")
    public MetaFinanceiraResponse atualizar(@PathVariable Long id, @Valid @RequestBody MetaFinanceiraRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
