package com.safrapay.backend.metafinanceira;

import com.safrapay.backend.metafinanceira.dto.MetaFinanceiraRequest;
import com.safrapay.backend.metafinanceira.dto.MetaFinanceiraResponse;
import com.safrapay.backend.metafinanceira.dto.MetaFinanceiraSyncRequest;
import com.safrapay.backend.metafinanceira.dto.MetaFinanceiraSyncResponse;
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
    public List<MetaFinanceiraResponse> listar(@RequestParam Long safraId) {
        return service.listarPorSafra(safraId);
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

    // --- Sincronizacao (offline-first) ---

    @PostMapping("/sync")
    public List<MetaFinanceiraSyncResponse> sincronizarPush(@RequestBody List<MetaFinanceiraSyncRequest> itens) {
        return service.sincronizarPush(itens);
    }

    @GetMapping("/sync")
    public List<MetaFinanceiraSyncResponse> sincronizarPull(@RequestParam(required = false) String desde) {
        return service.sincronizarPull(desde);
    }
}
