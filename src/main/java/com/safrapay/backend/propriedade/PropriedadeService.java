package com.safrapay.backend.propriedade;

import com.safrapay.backend.entity.Propriedade;
import com.safrapay.backend.entity.Usuario;
import com.safrapay.backend.propriedade.dto.PropriedadeRequest;
import com.safrapay.backend.propriedade.dto.PropriedadeResponse;
import com.safrapay.backend.repository.PropriedadeRepository;
import com.safrapay.backend.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PropriedadeService {

    private final PropriedadeRepository propriedadeRepository;

    public PropriedadeService(PropriedadeRepository propriedadeRepository) {
        this.propriedadeRepository = propriedadeRepository;
    }

    public List<PropriedadeResponse> listar() {
        Usuario usuario = SecurityUtils.usuarioLogado();
        return propriedadeRepository.findByUsuarioId(usuario.getId()).stream()
                .map(PropriedadeResponse::de)
                .toList();
    }

    public PropriedadeResponse buscar(Long id) {
        return PropriedadeResponse.de(buscarEntidade(id));
    }

    public PropriedadeResponse criar(PropriedadeRequest request) {
        Propriedade p = new Propriedade();
        p.setUsuario(SecurityUtils.usuarioLogado());
        aplicar(p, request);
        propriedadeRepository.save(p);
        return PropriedadeResponse.de(p);
    }

    public PropriedadeResponse atualizar(Long id, PropriedadeRequest request) {
        Propriedade p = buscarEntidade(id);
        aplicar(p, request);
        propriedadeRepository.save(p);
        return PropriedadeResponse.de(p);
    }

    public void deletar(Long id) {
        propriedadeRepository.delete(buscarEntidade(id));
    }

    /** Busca a propriedade garantindo que pertence ao usuario logado. */
    public Propriedade buscarEntidade(Long id) {
        Propriedade p = propriedadeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Propriedade nao encontrada"));

        if (!p.getUsuario().getId().equals(SecurityUtils.usuarioLogado().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Propriedade nao encontrada");
        }
        return p;
    }

    private void aplicar(Propriedade p, PropriedadeRequest request) {
        p.setNome(request.getNome());
        p.setLocalidade(request.getLocalidade());
        p.setAreaTotal(request.getAreaTotal());
        p.setTipoSolo(request.getTipoSolo());
        p.setMatricula(request.getMatricula());
        p.setCib(request.getCib());
        p.setValor(request.getValor());
        p.setObservacoes(request.getObservacoes());
    }
}
