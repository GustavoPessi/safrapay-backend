package com.safrapay.backend.safra;

import com.safrapay.backend.safra.dto.SafraRequest;
import com.safrapay.backend.safra.dto.SafraResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/safras")
public class SafraController {

    private final SafraService service;

    public SafraController(SafraService service) {
        this.service = service;
    }

    @GetMapping
    public List<SafraResponse> listar(@RequestParam Long talhaoId) {
        return service.listarPorTalhao(talhaoId);
    }

    @GetMapping("/{id}")
    public SafraResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<SafraResponse> criar(@Valid @RequestBody SafraRequest request) {
        return ResponseEntity.ok(service.criar(request));
    }

    @PutMapping("/{id}")
    public SafraResponse atualizar(@PathVariable Long id, @Valid @RequestBody SafraRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
