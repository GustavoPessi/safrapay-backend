package com.safrapay.backend.metafinanceira;

import com.safrapay.backend.entity.MetaFinanceira;
import com.safrapay.backend.entity.Safra;
import com.safrapay.backend.entity.Usuario;
import com.safrapay.backend.metafinanceira.dto.MetaFinanceiraRequest;
import com.safrapay.backend.metafinanceira.dto.MetaFinanceiraResponse;
import com.safrapay.backend.metafinanceira.dto.MetaFinanceiraSyncRequest;
import com.safrapay.backend.metafinanceira.dto.MetaFinanceiraSyncResponse;
import com.safrapay.backend.repository.MetaFinanceiraRepository;
import com.safrapay.backend.safra.SafraService;
import com.safrapay.backend.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class MetaFinanceiraService {

    private final MetaFinanceiraRepository repository;
    private final SafraService safraService;

    public MetaFinanceiraService(MetaFinanceiraRepository repository, SafraService safraService) {
        this.repository = repository;
        this.safraService = safraService;
    }

    public List<MetaFinanceiraResponse> listarPorSafra(Long safraId) {
        safraService.buscarEntidade(safraId);
        return repository.findBySafraId(safraId).stream().map(MetaFinanceiraResponse::de).toList();
    }

    public MetaFinanceiraResponse buscar(Long id) {
        return MetaFinanceiraResponse.de(buscarEntidade(id));
    }

    public MetaFinanceiraResponse criar(MetaFinanceiraRequest request) {
        MetaFinanceira m = new MetaFinanceira();
        m.setSafra(safraService.buscarEntidade(request.getSafraId()));
        aplicar(m, request);
        repository.save(m);
        return MetaFinanceiraResponse.de(m);
    }

    public MetaFinanceiraResponse atualizar(Long id, MetaFinanceiraRequest request) {
        MetaFinanceira m = buscarEntidade(id);
        if (!m.getSafra().getId().equals(request.getSafraId())) {
            m.setSafra(safraService.buscarEntidade(request.getSafraId()));
        }
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
        // valida que a safra (e portanto talhao/propriedade) pertence ao usuario logado
        safraService.buscarEntidade(m.getSafra().getId());
        return m;
    }

    private void aplicar(MetaFinanceira m, MetaFinanceiraRequest request) {
        m.setDescricao(request.getDescricao());
        m.setValorAlvo(request.getValorAlvo());
        m.setPrazo(request.getPrazo());
    }

    // ----------------------------------------------------------------------
    // Sincronizacao (Fase 3). Mesmo padrao de ReceitaService: safra-pai via
    // "safraUuid"; se ainda nao chegou no servidor, o item e pulado.
    // @Transactional porque MetaFinanceiraSyncResponse.de() le safra.getUuid()
    // via proxy lazy.
    // ----------------------------------------------------------------------

    @Transactional
    public List<MetaFinanceiraSyncResponse> sincronizarPush(List<MetaFinanceiraSyncRequest> itens) {
        Usuario usuario = SecurityUtils.usuarioLogado();
        List<MetaFinanceiraSyncResponse> respostas = new ArrayList<>();
        if (itens == null) return respostas;

        for (MetaFinanceiraSyncRequest item : itens) {
            if (item.getUuid() == null || item.getSafraUuid() == null
                    || item.getDescricao() == null || item.getDescricao().isBlank()
                    || item.getValorAlvo() == null) continue;

            Instant atualizadoCliente;
            try {
                atualizadoCliente = Instant.parse(item.getAtualizadoEm());
            } catch (Exception e) {
                continue;
            }

            Safra safra = safraService.buscarEntidadePorUuid(item.getSafraUuid());
            if (safra == null) continue; // safra-pai ainda nao sincronizou

            MetaFinanceira m = repository.findByUuidAndSafra_Talhao_Propriedade_Usuario_Id(item.getUuid(), usuario.getId()).orElse(null);
            if (m == null) {
                m = new MetaFinanceira();
                m.setUuid(item.getUuid());
                aplicarSync(m, item, safra);
                repository.save(m);
            } else if (atualizadoCliente.isAfter(m.getAtualizadoEm())) {
                aplicarSync(m, item, safra);
                repository.save(m);
            }
            respostas.add(MetaFinanceiraSyncResponse.de(m));
        }
        return respostas;
    }

    @Transactional(readOnly = true)
    public List<MetaFinanceiraSyncResponse> sincronizarPull(String desde) {
        Usuario usuario = SecurityUtils.usuarioLogado();
        List<MetaFinanceira> lista;
        if (desde == null || desde.isBlank()) {
            lista = repository.findBySafra_Talhao_Propriedade_Usuario_Id(usuario.getId());
        } else {
            Instant instanteDesde;
            try {
                instanteDesde = Instant.parse(desde);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parametro 'desde' invalido");
            }
            lista = repository.findBySafra_Talhao_Propriedade_Usuario_IdAndAtualizadoEmAfter(usuario.getId(), instanteDesde);
        }
        return lista.stream().map(MetaFinanceiraSyncResponse::de).toList();
    }

    private void aplicarSync(MetaFinanceira m, MetaFinanceiraSyncRequest item, Safra safra) {
        m.setSafra(safra);
        m.setDescricao(item.getDescricao());
        m.setValorAlvo(BigDecimal.valueOf(item.getValorAlvo()));
        m.setPrazo(parseData(item.getPrazo()));
    }

    private LocalDate parseData(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try { return LocalDate.parse(iso); }
        catch (Exception e) { return null; }
    }
}
