package com.safrapay.backend.metafinanceira;

import com.safrapay.backend.cultura.CulturaService;
import com.safrapay.backend.entity.Cultura;
import com.safrapay.backend.entity.MetaFinanceira;
import com.safrapay.backend.entity.Safra;
import com.safrapay.backend.metafinanceira.dto.MetaFinanceiraRequest;
import com.safrapay.backend.metafinanceira.dto.MetaFinanceiraResponse;
import com.safrapay.backend.repository.MetaFinanceiraRepository;
import com.safrapay.backend.safra.SafraService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MetaFinanceiraService {

    private final MetaFinanceiraRepository repository;
    private final CulturaService culturaService;
    private final SafraService safraService;

    public MetaFinanceiraService(MetaFinanceiraRepository repository, CulturaService culturaService, SafraService safraService) {
        this.repository = repository;
        this.culturaService = culturaService;
        this.safraService = safraService;
    }

    public List<MetaFinanceiraResponse> listarPorCultura(Long culturaId) {
        culturaService.buscarEntidade(culturaId);
        return repository.findByCulturaId(culturaId).stream().map(MetaFinanceiraResponse::de).toList();
    }

    public MetaFinanceiraResponse buscar(Long id) {
        return MetaFinanceiraResponse.de(buscarEntidade(id));
    }

    public MetaFinanceiraResponse criar(MetaFinanceiraRequest request) {
        MetaFinanceira m = new MetaFinanceira();
        preencherRelacionamentos(m, request);
        aplicar(m, request);
        repository.save(m);
        return MetaFinanceiraResponse.de(m);
    }

    public MetaFinanceiraResponse atualizar(Long id, MetaFinanceiraRequest request) {
        MetaFinanceira m = buscarEntidade(id);
        preencherRelacionamentos(m, request);
        aplicar(m, request);
        repository.save(m);
        return MetaFinanceiraResponse.de(m);
    }

    public void deletar(Long id) {
        repository.delete(buscarEntidade(id));
    }

    private MetaFinanceira buscarEntidade(Long id) {
        MetaFinanceira m = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meta financeira nao encontrada"));
        culturaService.buscarEntidade(m.getCultura().getId());
        return m;
    }

    private void preencherRelacionamentos(MetaFinanceira m, MetaFinanceiraRequest request) {
        Cultura cultura = culturaService.buscarEntidade(request.getCulturaId());
        m.setCultura(cultura);

        if (request.getSafraId() != null) {
            Safra safra = safraService.buscarEntidade(request.getSafraId());
            m.setSafra(safra);
        } else {
            m.setSafra(null);
        }
    }

    private void aplicar(MetaFinanceira m, MetaFinanceiraRequest request) {
        m.setDescricao(request.getDescricao());
        m.setValorAlvo(request.getValorAlvo());
        m.setPrazo(request.getPrazo());
    }
}
