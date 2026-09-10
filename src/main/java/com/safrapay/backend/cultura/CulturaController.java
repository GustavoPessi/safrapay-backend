package com.safrapay.backend.cultura;

import com.safrapay.backend.cultura.dto.CulturaRequest;
import com.safrapay.backend.cultura.dto.CulturaResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/culturas")
public class CulturaController {

    private final CulturaService service;

    public CulturaController(CulturaService service) {
        this.service = service;
    }

    @GetMapping
    public List<CulturaResponse> listar(@RequestParam Long propriedadeId) {
        return service.listar(propriedadeId);
    }

    @GetMapping("/{id}")
    public CulturaResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<CulturaResponse> criar(@Valid @RequestBody CulturaRequest request) {
        return ResponseEntity.ok(service.criar(request));
    }

    @PutMapping("/{id}")
    public CulturaResponse atualizar(@PathVariable Long id, @Valid @RequestBody CulturaRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
