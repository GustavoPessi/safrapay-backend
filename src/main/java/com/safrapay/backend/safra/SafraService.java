package com.safrapay.backend.safra;

import com.safrapay.backend.entity.Safra;
import com.safrapay.backend.entity.Talhao;
import com.safrapay.backend.entity.Usuario;
import com.safrapay.backend.repository.SafraRepository;
import com.safrapay.backend.safra.dto.SafraRequest;
import com.safrapay.backend.safra.dto.SafraResponse;
import com.safrapay.backend.safra.dto.SafraSyncRequest;
import com.safrapay.backend.safra.dto.SafraSyncResponse;
import com.safrapay.backend.security.SecurityUtils;
import com.safrapay.backend.talhao.TalhaoService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class SafraService {

    private final SafraRepository safraRepository;
    private final TalhaoService talhaoService;

    public SafraService(SafraRepository safraRepository, TalhaoService talhaoService) {
        this.safraRepository = safraRepository;
        this.talhaoService = talhaoService;
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
        Safra s = new Safra();
        s.setTalhao(talhao);
        aplicar(s, request);
        safraRepository.save(s);
        return SafraResponse.de(s);
    }

    public SafraResponse atualizar(Long id, SafraRequest request) {
        Safra s = buscarEntidade(id);
        if (!s.getTalhao().getId().equals(request.getTalhaoId())) {
            s.setTalhao(talhaoService.buscarEntidade(request.getTalhaoId()));
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
        s.setVariedade(request.getVariedade());
        s.setAnoSafra(request.getAnoSafra());
        s.setDataInicio(request.getDataInicio());
        s.setDataFim(request.getDataFim());
    }

    // ----------------------------------------------------------------------
    // Sincronizacao (push/pull). Mesmo padrao de TalhaoService (Fase 1): a
    // relacao com a safra-pai viaja como "talhaoUuid", resolvida aqui via
    // TalhaoService.buscarEntidadePorUuid. Se o talhao-pai ainda nao chegou
    // no servidor, o item e ignorado nesta passada - regra: talhao sincroniza
    // sempre antes de safra, e o app tenta de novo no proximo "Sincronizar".
    //
    // @Transactional aqui e necessario pelo mesmo motivo documentado em
    // TalhaoService: SafraSyncResponse.de() le s.getTalhao().getUuid(), que
    // exige inicializar o proxy lazy de verdade, e com
    // spring.jpa.open-in-view=false a sessao do Hibernate fecha assim que o
    // metodo do repositorio retorna.
    // ----------------------------------------------------------------------

    @Transactional
    public List<SafraSyncResponse> sincronizarPush(List<SafraSyncRequest> itens) {
        Usuario usuario = SecurityUtils.usuarioLogado();
        List<SafraSyncResponse> respostas = new ArrayList<>();
        if (itens == null) return respostas;

        for (SafraSyncRequest item : itens) {
            if (item.getUuid() == null || item.getTalhaoUuid() == null
                    || item.getAnoSafra() == null || item.getAnoSafra().isBlank()) continue;

            Instant atualizadoCliente;
            try {
                atualizadoCliente = Instant.parse(item.getAtualizadoEm());
            } catch (Exception e) {
                continue;
            }

            Talhao talhao = talhaoService.buscarEntidadePorUuid(item.getTalhaoUuid());
            if (talhao == null) {
                continue; // talhao-pai ainda nao sincronizou - tenta de novo depois
            }

            Safra s = safraRepository.findByUuidAndTalhao_Propriedade_Usuario_Id(item.getUuid(), usuario.getId()).orElse(null);
            if (s == null) {
                s = new Safra();
                s.setUuid(item.getUuid());
                s.setTalhao(talhao);
                aplicarSync(s, item, talhao);
                safraRepository.save(s);
            } else if (atualizadoCliente.isAfter(s.getAtualizadoEm())) {
                aplicarSync(s, item, talhao);
                safraRepository.save(s);
            }
            respostas.add(SafraSyncResponse.de(s));
        }
        return respostas;
    }

    @Transactional(readOnly = true)
    public List<SafraSyncResponse> sincronizarPull(String desde) {
        Usuario usuario = SecurityUtils.usuarioLogado();
        List<Safra> lista;
        if (desde == null || desde.isBlank()) {
            lista = safraRepository.findByTalhao_Propriedade_Usuario_Id(usuario.getId());
        } else {
            Instant instanteDesde;
            try {
                instanteDesde = Instant.parse(desde);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parametro 'desde' invalido");
            }
            lista = safraRepository.findByTalhao_Propriedade_Usuario_IdAndAtualizadoEmAfter(usuario.getId(), instanteDesde);
        }
        return lista.stream().map(SafraSyncResponse::de).toList();
    }

    private void aplicarSync(Safra s, SafraSyncRequest item, Talhao talhao) {
        s.setTalhao(talhao);
        s.setVariedade(item.getVariedade());
        s.setAnoSafra(item.getAnoSafra());
        s.setDataInicio(parseData(item.getDataInicio()));
        s.setDataFim(parseData(item.getDataFim()));
    }

    private LocalDate parseData(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try { return LocalDate.parse(iso); }
        catch (Exception e) { return null; }
    }
}
