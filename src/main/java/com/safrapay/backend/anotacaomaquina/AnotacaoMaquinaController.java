package com.safrapay.backend.anotacaomaquina;

import com.safrapay.backend.anotacaomaquina.dto.AnotacaoMaquinaRequest;
import com.safrapay.backend.anotacaomaquina.dto.AnotacaoMaquinaResponse;
import com.safrapay.backend.anotacaomaquina.dto.AnotacaoMaquinaSyncRequest;
import com.safrapay.backend.anotacaomaquina.dto.AnotacaoMaquinaSyncResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/anotacoes-maquina")
public class AnotacaoMaquinaController {

    private final AnotacaoMaquinaService service;

    public AnotacaoMaquinaController(AnotacaoMaquinaService service) {
        this.service = service;
    }

    @GetMapping
    public List<AnotacaoMaquinaResponse> listar(@RequestParam Long maquinaId) {
        return service.listarPorMaquina(maquinaId);
    }

    @GetMapping("/{id}")
    public AnotacaoMaquinaResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<AnotacaoMaquinaResponse> criar(@Valid @RequestBody AnotacaoMaquinaRequest request) {
        return ResponseEntity.ok(service.criar(request));
    }

    @PutMapping("/{id}")
    public AnotacaoMaquinaResponse atualizar(@PathVariable Long id, @Valid @RequestBody AnotacaoMaquinaRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }

    // --- Sincronizacao (offline-first) ---

    @PostMapping("/sync")
    public List<AnotacaoMaquinaSyncResponse> sincronizarPush(@RequestBody List<AnotacaoMaquinaSyncRequest> itens) {
        return service.sincronizarPush(itens);
    }

    @GetMapping("/sync")
    public List<AnotacaoMaquinaSyncResponse> sincronizarPull(@RequestParam(required = false) String desde) {
        return service.sincronizarPull(desde);
    }
}
