package com.safrapay.backend.anotacaomaquina;

import com.safrapay.backend.anotacaomaquina.dto.AnotacaoMaquinaRequest;
import com.safrapay.backend.anotacaomaquina.dto.AnotacaoMaquinaResponse;
import com.safrapay.backend.entity.AnotacaoMaquina;
import com.safrapay.backend.entity.Maquina;
import com.safrapay.backend.maquina.MaquinaService;
import com.safrapay.backend.repository.AnotacaoMaquinaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AnotacaoMaquinaService {

    private final AnotacaoMaquinaRepository repository;
    private final MaquinaService maquinaService;

    public AnotacaoMaquinaService(AnotacaoMaquinaRepository repository, MaquinaService maquinaService) {
        this.repository = repository;
        this.maquinaService = maquinaService;
    }

    public List<AnotacaoMaquinaResponse> listarPorMaquina(Long maquinaId) {
        maquinaService.buscarEntidade(maquinaId);
        return repository.findByMaquinaId(maquinaId).stream().map(AnotacaoMaquinaResponse::de).toList();
    }

    public AnotacaoMaquinaResponse buscar(Long id) {
        return AnotacaoMaquinaResponse.de(buscarEntidade(id));
    }

    public AnotacaoMaquinaResponse criar(AnotacaoMaquinaRequest request) {
        Maquina maquina = maquinaService.buscarEntidade(request.getMaquinaId());
        AnotacaoMaquina a = new AnotacaoMaquina();
        a.setMaquina(maquina);
        aplicar(a, request);
        repository.save(a);
        return AnotacaoMaquinaResponse.de(a);
    }

    public AnotacaoMaquinaResponse atualizar(Long id, AnotacaoMaquinaRequest request) {
        AnotacaoMaquina a = buscarEntidade(id);
        if (!a.getMaquina().getId().equals(request.getMaquinaId())) {
            a.setMaquina(maquinaService.buscarEntidade(request.getMaquinaId()));
        }
        aplicar(a, request);
        repository.save(a);
        return AnotacaoMaquinaResponse.de(a);
    }

    public void deletar(Long id) {
        repository.delete(buscarEntidade(id));
    }

    private AnotacaoMaquina buscarEntidade(Long id) {
        AnotacaoMaquina a = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anotacao nao encontrada"));
        maquinaService.buscarEntidade(a.getMaquina().getId());
        return a;
    }

    private void aplicar(AnotacaoMaquina a, AnotacaoMaquinaRequest request) {
        a.setTexto(request.getTexto());
        a.setData(request.getData());
    }
}
