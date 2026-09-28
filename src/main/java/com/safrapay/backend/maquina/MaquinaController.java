package com.safrapay.backend.maquina;

import com.safrapay.backend.maquina.dto.MaquinaRequest;
import com.safrapay.backend.maquina.dto.MaquinaResponse;
import com.safrapay.backend.maquina.dto.MaquinaSyncRequest;
import com.safrapay.backend.maquina.dto.MaquinaSyncResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maquinas")
public class MaquinaController {

    private final MaquinaService service;

    public MaquinaController(MaquinaService service) {
        this.service = service;
    }

    @GetMapping
    public List<MaquinaResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public MaquinaResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<MaquinaResponse> criar(@Valid @RequestBody MaquinaRequest request) {
        return ResponseEntity.ok(service.criar(request));
    }

    @PutMapping("/{id}")
    public MaquinaResponse atualizar(@PathVariable Long id, @Valid @RequestBody MaquinaRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }

    // --- Sincronizacao (offline-first) ---

    @PostMapping("/sync")
    public List<MaquinaSyncResponse> sincronizarPush(@RequestBody List<MaquinaSyncRequest> itens) {
        return service.sincronizarPush(itens);
    }

    @GetMapping("/sync")
    public List<MaquinaSyncResponse> sincronizarPull(@RequestParam(required = false) String desde) {
        return service.sincronizarPull(desde);
    }
}
