package com.safrapay.backend.propriedade;

import com.safrapay.backend.propriedade.dto.PropriedadeRequest;
import com.safrapay.backend.propriedade.dto.PropriedadeResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/propriedades")
public class PropriedadeController {

    private final PropriedadeService service;

    public PropriedadeController(PropriedadeService service) {
        this.service = service;
    }

    @GetMapping
    public List<PropriedadeResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public PropriedadeResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<PropriedadeResponse> criar(@Valid @RequestBody PropriedadeRequest request) {
        return ResponseEntity.ok(service.criar(request));
    }

    @PutMapping("/{id}")
    public PropriedadeResponse atualizar(@PathVariable Long id, @Valid @RequestBody PropriedadeRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
