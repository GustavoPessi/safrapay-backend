package com.safrapay.backend.talhao;

import com.safrapay.backend.talhao.dto.TalhaoRequest;
import com.safrapay.backend.talhao.dto.TalhaoResponse;
import com.safrapay.backend.talhao.dto.TalhaoSyncRequest;
import com.safrapay.backend.talhao.dto.TalhaoSyncResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/talhoes")
public class TalhaoController {

    private final TalhaoService service;

    public TalhaoController(TalhaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<TalhaoResponse> listar(@RequestParam Long propriedadeId) {
        return service.listar(propriedadeId);
    }

    @GetMapping("/{id}")
    public TalhaoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<TalhaoResponse> criar(@Valid @RequestBody TalhaoRequest request) {
        return ResponseEntity.ok(service.criar(request));
    }

    @PutMapping("/{id}")
    public TalhaoResponse atualizar(@PathVariable Long id, @Valid @RequestBody TalhaoRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }

    // --- Sincronizacao (offline-first) ---

    @PostMapping("/sync")
    public List<TalhaoSyncResponse> sincronizarPush(@RequestBody List<TalhaoSyncRequest> itens) {
        return service.sincronizarPush(itens);
    }

    @GetMapping("/sync")
    public List<TalhaoSyncResponse> sincronizarPull(@RequestParam(required = false) String desde) {
        return service.sincronizarPull(desde);
    }
}
