package com.safrapay.backend.receita;

import com.safrapay.backend.entity.Receita;
import com.safrapay.backend.entity.Safra;
import com.safrapay.backend.receita.dto.ReceitaRequest;
import com.safrapay.backend.receita.dto.ReceitaResponse;
import com.safrapay.backend.repository.ReceitaRepository;
import com.safrapay.backend.safra.SafraService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ReceitaService {

    private final ReceitaRepository receitaRepository;
    private final SafraService safraService;

    public ReceitaService(ReceitaRepository receitaRepository, SafraService safraService) {
        this.receitaRepository = receitaRepository;
        this.safraService = safraService;
    }

    public List<ReceitaResponse> listarPorSafra(Long safraId) {
        safraService.buscarEntidade(safraId);
        return receitaRepository.findBySafraId(safraId).stream().map(ReceitaResponse::de).toList();
    }

    public ReceitaResponse buscar(Long id) {
        return ReceitaResponse.de(buscarEntidade(id));
    }

    public ReceitaResponse criar(ReceitaRequest request) {
        Safra safra = safraService.buscarEntidade(request.getSafraId());
        Receita r = new Receita();
        r.setSafra(safra);
        aplicar(r, request);
        receitaRepository.save(r);
        return ReceitaResponse.de(r);
    }

    public ReceitaResponse atualizar(Long id, ReceitaRequest request) {
        Receita r = buscarEntidade(id);
        if (!r.getSafra().getId().equals(request.getSafraId())) {
            r.setSafra(safraService.buscarEntidade(request.getSafraId()));
        }
        aplicar(r, request);
        receitaRepository.save(r);
        return ReceitaResponse.de(r);
    }

    public void deletar(Long id) {
        receitaRepository.delete(buscarEntidade(id));
    }

    private Receita buscarEntidade(Long id) {
        Receita r = receitaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receita nao encontrada"));
        safraService.buscarEntidade(r.getSafra().getId());
        return r;
    }

    private void aplicar(Receita r, ReceitaRequest request) {
        r.setDescricao(request.getDescricao());
        r.setValor(request.getValor());
        r.setDataRegistro(request.getDataRegistro());
    }
}
