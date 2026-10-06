package com.safrapay.backend.colaborador;

import com.safrapay.backend.colaborador.dto.MaquinaColaboradorResponse;
import com.safrapay.backend.colaborador.dto.ReportarRequest;
import com.safrapay.backend.colaborador.dto.TarefaResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Telas online do colaborador (35/36 do Figma). Liberado pra qualquer papel em
 * SecurityConfig.ROTAS_COLABORADOR. Erros saem como {status, mensagem}.
 */
@RestController
@RequestMapping("/api/colaborador")
public class ColaboradorController {

    private final ColaboradorService service;

    public ColaboradorController(ColaboradorService service) {
        this.service = service;
    }

    @GetMapping("/maquinas")
    public List<MaquinaColaboradorResponse> maquinas() {
        return service.maquinas();
    }

    @GetMapping("/tarefas")
    public List<TarefaResponse> tarefas(@RequestParam(required = false) String filtro,
                                        @RequestParam(required = false) String maquinaUuid) {
        return service.tarefas(filtro, maquinaUuid);
    }

    @PostMapping("/tarefas/{uuid}/concluir")
    public TarefaResponse concluir(@PathVariable String uuid) {
        return service.concluir(uuid);
    }

    @PostMapping("/tarefas/{uuid}/reabrir")
    public TarefaResponse reabrir(@PathVariable String uuid) {
        return service.reabrir(uuid);
    }

    @PostMapping("/maquinas/{uuid}/tarefas")
    @ResponseStatus(HttpStatus.CREATED)
    public TarefaResponse reportar(@PathVariable String uuid, @RequestBody ReportarRequest request) {
        return service.reportar(uuid, request);
    }
}
