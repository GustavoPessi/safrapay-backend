package com.safrapay.backend.safra;

import com.safrapay.backend.cultura.CulturaService;
import com.safrapay.backend.entity.Cultura;
import com.safrapay.backend.entity.Safra;
import com.safrapay.backend.entity.Talhao;
import com.safrapay.backend.repository.SafraRepository;
import com.safrapay.backend.safra.dto.SafraRequest;
import com.safrapay.backend.safra.dto.SafraResponse;
import com.safrapay.backend.talhao.TalhaoService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SafraService {

    private final SafraRepository safraRepository;
    private final TalhaoService talhaoService;
    private final CulturaService culturaService;

    public SafraService(SafraRepository safraRepository, TalhaoService talhaoService, CulturaService culturaService) {
        this.safraRepository = safraRepository;
        this.talhaoService = talhaoService;
        this.culturaService = culturaService;
    }

    public List<SafraResponse> listarPorTalhao(Long talhaoId) {
        talhaoService.buscarEntidade(talhaoId);
        return safraRepository.findByTalhaoId(talhaoId).stream().map(SafraResponse::de).toList();
    }

    public SafraResponse buscar(Long id) {
        return SafraResponse.de(buscarEntidade(id));
    }

    public SafraResponse criar(SafraRequest request) {
        Talhao talhao = talhaoService.buscarEntidade(request.getTalhaoId());
        Cultura cultura = culturaService.buscarEntidade(request.getCulturaId());
        Safra s = new Safra();
        s.setTalhao(talhao);
        s.setCultura(cultura);
        aplicar(s, request);
        safraRepository.save(s);
        return SafraResponse.de(s);
    }

    public SafraResponse atualizar(Long id, SafraRequest request) {
        Safra s = buscarEntidade(id);
        if (!s.getTalhao().getId().equals(request.getTalhaoId())) {
            s.setTalhao(talhaoService.buscarEntidade(request.getTalhaoId()));
        }
        if (!s.getCultura().getId().equals(request.getCulturaId())) {
            s.setCultura(culturaService.buscarEntidade(request.getCulturaId()));
        }
        aplicar(s, request);
        safraRepository.save(s);
        return SafraResponse.de(s);
    }

    public void deletar(Long id) {
        safraRepository.delete(buscarEntidade(id));
    }

    public Safra buscarEntidade(Long id) {
        Safra s = safraRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Safra nao encontrada"));
        // valida que o talhao (e portanto a propriedade) pertence ao usuario logado
        talhaoService.buscarEntidade(s.getTalhao().getId());
        return s;
    }

    private void aplicar(Safra s, SafraRequest request) {
        s.setAnoSafra(request.getAnoSafra());
        s.setDataInicio(request.getDataInicio());
        s.setDataFim(request.getDataFim());
    }
}
