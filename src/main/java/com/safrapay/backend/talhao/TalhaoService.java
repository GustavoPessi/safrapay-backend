package com.safrapay.backend.talhao;

import com.safrapay.backend.entity.Propriedade;
import com.safrapay.backend.entity.Talhao;
import com.safrapay.backend.propriedade.PropriedadeService;
import com.safrapay.backend.repository.TalhaoRepository;
import com.safrapay.backend.talhao.dto.TalhaoRequest;
import com.safrapay.backend.talhao.dto.TalhaoResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TalhaoService {

    private final TalhaoRepository talhaoRepository;
    private final PropriedadeService propriedadeService;

    public TalhaoService(TalhaoRepository talhaoRepository, PropriedadeService propriedadeService) {
        this.talhaoRepository = talhaoRepository;
        this.propriedadeService = propriedadeService;
    }

    public List<TalhaoResponse> listar(Long propriedadeId) {
        // garante que a propriedade pertence ao usuario logado antes de listar
        propriedadeService.buscarEntidade(propriedadeId);
        return talhaoRepository.findByPropriedadeId(propriedadeId).stream()
                .map(TalhaoResponse::de)
                .toList();
    }

    public TalhaoResponse buscar(Long id) {
        return TalhaoResponse.de(buscarEntidade(id));
    }

    public TalhaoResponse criar(TalhaoRequest request) {
        Propriedade propriedade = propriedadeService.buscarEntidade(request.getPropriedadeId());
        Talhao t = new Talhao();
        t.setPropriedade(propriedade);
        aplicar(t, request);
        talhaoRepository.save(t);
        return TalhaoResponse.de(t);
    }

    public TalhaoResponse atualizar(Long id, TalhaoRequest request) {
        Talhao t = buscarEntidade(id);
        // se o propriedadeId mudou, revalida o dono da nova propriedade
        if (!t.getPropriedade().getId().equals(request.getPropriedadeId())) {
            t.setPropriedade(propriedadeService.buscarEntidade(request.getPropriedadeId()));
        }
        aplicar(t, request);
        talhaoRepository.save(t);
        return TalhaoResponse.de(t);
    }

    public void deletar(Long id) {
        talhaoRepository.delete(buscarEntidade(id));
    }

    public Talhao buscarEntidade(Long id) {
        Talhao t = talhaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Talhao nao encontrado"));
        // dispara 404 se a propriedade-mae nao for do usuario logado
        propriedadeService.buscarEntidade(t.getPropriedade().getId());
        return t;
    }

    private void aplicar(Talhao t, TalhaoRequest request) {
        t.setNome(request.getNome());
        t.setAreaHa(request.getAreaHa());
    }
}
