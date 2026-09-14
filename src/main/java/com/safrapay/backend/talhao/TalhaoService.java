package com.safrapay.backend.talhao;

import com.safrapay.backend.entity.Propriedade;
import com.safrapay.backend.entity.Talhao;
import com.safrapay.backend.entity.Usuario;
import com.safrapay.backend.propriedade.PropriedadeService;
import com.safrapay.backend.repository.TalhaoRepository;
import com.safrapay.backend.security.SecurityUtils;
import com.safrapay.backend.talhao.dto.TalhaoRequest;
import com.safrapay.backend.talhao.dto.TalhaoResponse;
import com.safrapay.backend.talhao.dto.TalhaoSyncRequest;
import com.safrapay.backend.talhao.dto.TalhaoSyncResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
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

    // ----------------------------------------------------------------------
    // Sincronizacao (push/pull). Segue o mesmo padrao de PropriedadeService,
    // com uma diferenca: a relacao com a propriedade-mae viaja como
    // "propriedadeUuid" (nunca um id local do celular ou do servidor), e e
    // resolvida aqui via PropriedadeService.buscarEntidadePorUuid. Se a
    // propriedade-mae ainda nao chegou no servidor, o item e ignorado nesta
    // passada - regra: propriedade sincroniza sempre antes de talhao, e o
    // app tenta de novo no proximo "Sincronizar".
    //
    // @Transactional aqui e necessario: TalhaoSyncResponse.de() le
    // t.getPropriedade().getUuid() (nao so o id - que vem "de graca" no proxy
    // sem tocar o banco). O uuid exige inicializar o proxy lazy de verdade, e
    // com spring.jpa.open-in-view=false a sessao do Hibernate fecha assim que
    // o metodo do repositorio retorna. Sem a transacao abrangendo o metodo
    // inteiro, isso estoura LazyInitializationException e a chamada cai como
    // erro 500 (foi o bug que causou "Falha ao buscar talhoes do servidor" -
    // corrigido em 14/09/2026).
    // ----------------------------------------------------------------------

    @Transactional
    public List<TalhaoSyncResponse> sincronizarPush(List<TalhaoSyncRequest> itens) {
        Usuario usuario = SecurityUtils.usuarioLogado();
        List<TalhaoSyncResponse> respostas = new ArrayList<>();
        if (itens == null) return respostas;

        for (TalhaoSyncRequest item : itens) {
            if (item.getUuid() == null || item.getPropriedadeUuid() == null
                    || item.getNome() == null || item.getNome().isBlank()) continue;

            Instant atualizadoCliente;
            try {
                atualizadoCliente = Instant.parse(item.getAtualizadoEm());
            } catch (Exception e) {
                continue;
            }

            Propriedade propriedade = propriedadeService.buscarEntidadePorUuid(item.getPropriedadeUuid());
            if (propriedade == null) {
                continue; // propriedade-mae ainda nao sincronizou - tenta de novo depois
            }

            Talhao t = talhaoRepository.findByUuidAndPropriedade_Usuario_Id(item.getUuid(), usuario.getId()).orElse(null);
            if (t == null) {
                t = new Talhao();
                t.setUuid(item.getUuid());
                t.setPropriedade(propriedade);
                aplicarSync(t, item, propriedade);
                talhaoRepository.save(t);
            } else if (atualizadoCliente.isAfter(t.getAtualizadoEm())) {
                aplicarSync(t, item, propriedade);
                talhaoRepository.save(t);
            }
            respostas.add(TalhaoSyncResponse.de(t));
        }
        return respostas;
    }

    @Transactional(readOnly = true)
    public List<TalhaoSyncResponse> sincronizarPull(String desde) {
        Usuario usuario = SecurityUtils.usuarioLogado();
        List<Talhao> lista;
        if (desde == null || desde.isBlank()) {
            lista = talhaoRepository.findByPropriedade_Usuario_Id(usuario.getId());
        } else {
            Instant instanteDesde;
            try {
                instanteDesde = Instant.parse(desde);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parametro 'desde' invalido");
            }
            lista = talhaoRepository.findByPropriedade_Usuario_IdAndAtualizadoEmAfter(usuario.getId(), instanteDesde);
        }
        return lista.stream().map(TalhaoSyncResponse::de).toList();
    }

    private void aplicarSync(Talhao t, TalhaoSyncRequest item, Propriedade propriedade) {
        t.setPropriedade(propriedade);
        t.setNome(item.getNome());
        t.setAreaHa(item.getAreaHa());
        t.setCultura(item.getCultura());
    }
}
