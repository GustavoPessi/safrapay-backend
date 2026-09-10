package com.safrapay.backend.cultura;

import com.safrapay.backend.cultura.dto.CulturaRequest;
import com.safrapay.backend.cultura.dto.CulturaResponse;
import com.safrapay.backend.entity.Cultura;
import com.safrapay.backend.entity.Propriedade;
import com.safrapay.backend.propriedade.PropriedadeService;
import com.safrapay.backend.repository.CulturaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CulturaService {

    private final CulturaRepository culturaRepository;
    private final PropriedadeService propriedadeService;

    public CulturaService(CulturaRepository culturaRepository, PropriedadeService propriedadeService) {
        this.culturaRepository = culturaRepository;
        this.propriedadeService = propriedadeService;
    }

    public List<CulturaResponse> listar(Long propriedadeId) {
        propriedadeService.buscarEntidade(propriedadeId);
        return culturaRepository.findByPropriedadeId(propriedadeId).stream()
                .map(CulturaResponse::de)
                .toList();
    }

    public CulturaResponse buscar(Long id) {
        return CulturaResponse.de(buscarEntidade(id));
    }

    public CulturaResponse criar(CulturaRequest request) {
        Propriedade propriedade = propriedadeService.buscarEntidade(request.getPropriedadeId());
        Cultura c = new Cultura();
        c.setPropriedade(propriedade);
        c.setNome(request.getNome());
        culturaRepository.save(c);
        return CulturaResponse.de(c);
    }

    public CulturaResponse atualizar(Long id, CulturaRequest request) {
        Cultura c = buscarEntidade(id);
        if (!c.getPropriedade().getId().equals(request.getPropriedadeId())) {
            c.setPropriedade(propriedadeService.buscarEntidade(request.getPropriedadeId()));
        }
        c.setNome(request.getNome());
        culturaRepository.save(c);
        return CulturaResponse.de(c);
    }

    public void deletar(Long id) {
        culturaRepository.delete(buscarEntidade(id));
    }

    public Cultura buscarEntidade(Long id) {
        Cultura c = culturaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cultura nao encontrada"));
        propriedadeService.buscarEntidade(c.getPropriedade().getId());
        return c;
    }
}
