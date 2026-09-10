package com.safrapay.backend.maquina;

import com.safrapay.backend.entity.Maquina;
import com.safrapay.backend.entity.Usuario;
import com.safrapay.backend.maquina.dto.MaquinaRequest;
import com.safrapay.backend.maquina.dto.MaquinaResponse;
import com.safrapay.backend.repository.MaquinaRepository;
import com.safrapay.backend.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MaquinaService {

    private final MaquinaRepository maquinaRepository;

    public MaquinaService(MaquinaRepository maquinaRepository) {
        this.maquinaRepository = maquinaRepository;
    }

    public List<MaquinaResponse> listar() {
        Usuario usuario = SecurityUtils.usuarioLogado();
        return maquinaRepository.findByUsuarioId(usuario.getId()).stream()
                .map(MaquinaResponse::de)
                .toList();
    }

    public MaquinaResponse buscar(Long id) {
        return MaquinaResponse.de(buscarEntidade(id));
    }

    public MaquinaResponse criar(MaquinaRequest request) {
        Maquina m = new Maquina();
        m.setUsuario(SecurityUtils.usuarioLogado());
        aplicar(m, request);
        maquinaRepository.save(m);
        return MaquinaResponse.de(m);
    }

    public MaquinaResponse atualizar(Long id, MaquinaRequest request) {
        Maquina m = buscarEntidade(id);
        aplicar(m, request);
        maquinaRepository.save(m);
        return MaquinaResponse.de(m);
    }

    public void deletar(Long id) {
        maquinaRepository.delete(buscarEntidade(id));
    }

    /** Busca a maquina garantindo que pertence ao usuario logado. */
    public Maquina buscarEntidade(Long id) {
        Maquina m = maquinaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Maquina nao encontrada"));

        if (!m.getUsuario().getId().equals(SecurityUtils.usuarioLogado().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Maquina nao encontrada");
        }
        return m;
    }

    private void aplicar(Maquina m, MaquinaRequest request) {
        m.setNome(request.getNome());
        m.setMarca(request.getMarca());
        m.setTipo(request.getTipo());
        m.setAno(request.getAno());
        m.setIdentificacao(request.getIdentificacao());
        m.setValor(request.getValor());
        m.setObservacoes(request.getObservacoes());
    }
}
