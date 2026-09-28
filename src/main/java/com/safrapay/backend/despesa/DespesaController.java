package com.safrapay.backend.despesa;

import com.safrapay.backend.despesa.dto.DespesaRequest;
import com.safrapay.backend.despesa.dto.DespesaResponse;
import com.safrapay.backend.despesa.dto.DespesaSyncRequest;
import com.safrapay.backend.despesa.dto.DespesaSyncResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/despesas")
public class DespesaController {

    private final DespesaService service;

    public DespesaController(DespesaService service) {
        this.service = service;
    }

    @GetMapping
    public List<DespesaResponse> listar(@RequestParam Long safraId) {
        return service.listarPorSafra(safraId);
    }

    @GetMapping("/{id}")
    public DespesaResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<DespesaResponse> criar(@Valid @RequestBody DespesaRequest request) {
        return ResponseEntity.ok(service.criar(request));
    }

    @PutMapping("/{id}")
    public DespesaResponse atualizar(@PathVariable Long id, @Valid @RequestBody DespesaRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }

    // --- Sincronizacao (offline-first) ---

    @PostMapping("/sync")
    public List<DespesaSyncResponse> sincronizarPush(@RequestBody List<DespesaSyncRequest> itens) {
        return service.sincronizarPush(itens);
    }

    @GetMapping("/sync")
    public List<DespesaSyncResponse> sincronizarPull(@RequestParam(required = false) String desde) {
        return service.sincronizarPull(desde);
    }
}
