package com.safrapay.backend.equipe;

import com.safrapay.backend.equipe.dto.ConviteRecebidoResponse;
import com.safrapay.backend.equipe.dto.ConviteRequest;
import com.safrapay.backend.equipe.dto.EquipeMembroResponse;
import com.safrapay.backend.equipe.dto.MinhaEquipeResponse;
import com.safrapay.backend.equipe.dto.PapelRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Equipe da fazenda (multiusuário, 06/10/2026). Quais rotas o colaborador
 * alcança está em SecurityConfig.ROTAS_COLABORADOR; as de gestão checam o
 * papel DONO no service. Erros saem como {status, mensagem} (ErrosMultiusuarioAdvice).
 */
@RestController
@RequestMapping("/api/equipe")
public class EquipeController {

    private final EquipeService service;

    public EquipeController(EquipeService service) {
        this.service = service;
    }

    // Qualquer papel

    @GetMapping("/minha")
    public MinhaEquipeResponse minha() {
        return service.minha();
    }

    @GetMapping("/convites/recebidos")
    public List<ConviteRecebidoResponse> recebidos() {
        return service.recebidos();
    }

    @PostMapping("/convites/{uuid}/aceitar")
    public MinhaEquipeResponse aceitar(@PathVariable String uuid) {
        return service.aceitar(uuid);
    }

    @PostMapping("/convites/{uuid}/recusar")
    public void recusar(@PathVariable String uuid) {
        service.recusar(uuid);
    }

    @PostMapping("/sair")
    public MinhaEquipeResponse sair() {
        return service.sair();
    }

    // Só o dono

    @GetMapping
    public List<EquipeMembroResponse> listar() {
        return service.listar();
    }

    @PostMapping("/convites")
    @ResponseStatus(HttpStatus.CREATED)
    public EquipeMembroResponse convidar(@RequestBody ConviteRequest request) {
        return service.convidar(request);
    }

    @PatchMapping("/membros/{uuid}")
    public EquipeMembroResponse alterarPapel(@PathVariable String uuid, @RequestBody PapelRequest request) {
        return service.alterarPapel(uuid, request);
    }

    @DeleteMapping("/membros/{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable String uuid) {
        service.remover(uuid);
    }
}
