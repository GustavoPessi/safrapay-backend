package com.safrapay.backend.despesa;

import com.safrapay.backend.despesa.dto.DespesaRequest;
import com.safrapay.backend.despesa.dto.DespesaResponse;
import com.safrapay.backend.entity.CategoriaDespesa;
import com.safrapay.backend.entity.Despesa;
import com.safrapay.backend.entity.Maquina;
import com.safrapay.backend.entity.Safra;
import com.safrapay.backend.maquina.MaquinaService;
import com.safrapay.backend.repository.CategoriaDespesaRepository;
import com.safrapay.backend.repository.DespesaRepository;
import com.safrapay.backend.safra.SafraService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DespesaService {

    private final DespesaRepository despesaRepository;
    private final CategoriaDespesaRepository categoriaDespesaRepository;
    private final SafraService safraService;
    private final MaquinaService maquinaService;

    public DespesaService(DespesaRepository despesaRepository,
                           CategoriaDespesaRepository categoriaDespesaRepository,
                           SafraService safraService,
                           MaquinaService maquinaService) {
        this.despesaRepository = despesaRepository;
        this.categoriaDespesaRepository = categoriaDespesaRepository;
        this.safraService = safraService;
        this.maquinaService = maquinaService;
    }

    public List<DespesaResponse> listarPorSafra(Long safraId) {
        safraService.buscarEntidade(safraId);
        return despesaRepository.findBySafraId(safraId).stream().map(DespesaResponse::de).toList();
    }

    public DespesaResponse buscar(Long id) {
        return DespesaResponse.de(buscarEntidade(id));
    }

    public DespesaResponse criar(DespesaRequest request) {
        Despesa d = new Despesa();
        preencherRelacionamentos(d, request);
        aplicar(d, request);
        despesaRepository.save(d);
        return DespesaResponse.de(d);
    }

    public DespesaResponse atualizar(Long id, DespesaRequest request) {
        Despesa d = buscarEntidade(id);
        preencherRelacionamentos(d, request);
        aplicar(d, request);
        despesaRepository.save(d);
        return DespesaResponse.de(d);
    }

    public void deletar(Long id) {
        despesaRepository.delete(buscarEntidade(id));
    }

    private Despesa buscarEntidade(Long id) {
        Despesa d = despesaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Despesa nao encontrada"));
        safraService.buscarEntidade(d.getSafra().getId());
        return d;
    }

    private void preencherRelacionamentos(Despesa d, DespesaRequest request) {
        Safra safra = safraService.buscarEntidade(request.getSafraId());
        CategoriaDespesa categoria = categoriaDespesaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria nao encontrada"));
        d.setSafra(safra);
        d.setCategoria(categoria);

        if (request.getMaquinaId() != null) {
            Maquina maquina = maquinaService.buscarEntidade(request.getMaquinaId());
            d.setMaquina(maquina);
        } else {
            d.setMaquina(null);
        }
    }

    private void aplicar(Despesa d, DespesaRequest request) {
        d.setDescricao(request.getDescricao());
        d.setValor(request.getValor());
        d.setDataRegistro(request.getDataRegistro());
    }
}
