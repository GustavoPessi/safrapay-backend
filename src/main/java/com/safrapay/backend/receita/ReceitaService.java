package com.safrapay.backend.receita;

import com.safrapay.backend.entity.Receita;
import com.safrapay.backend.entity.Safra;
import com.safrapay.backend.entity.Usuario;
import com.safrapay.backend.receita.dto.ReceitaRequest;
import com.safrapay.backend.receita.dto.ReceitaResponse;
import com.safrapay.backend.receita.dto.ReceitaSyncRequest;
import com.safrapay.backend.receita.dto.ReceitaSyncResponse;
import com.safrapay.backend.repository.ReceitaRepository;
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
public class ReceitaService {

    private final ReceitaRepository receitaRepository;
    private final SafraService safraService;

    public ReceitaService(ReceitaRepository receitaRepository, SafraService safraService) {
        this.receitaRepository = receitaRepository;
        this.safraService = safraService;
    }

    public List<ReceitaResponse> listarPorSafra(Long safraId) {
        safraService.buscarEntidade(safraId);
        return receitaRepository.findBySafraId(safraId).stream().map(ReceitaResponse::de).toList();
    }

    public ReceitaResponse buscar(Long id) {
        return ReceitaResponse.de(buscarEntidade(id));
    }

    public ReceitaResponse criar(ReceitaRequest request) {
        Safra safra = safraService.buscarEntidade(request.getSafraId());
        Receita r = new Receita();
        r.setSafra(safra);
        aplicar(r, request);
        receitaRepository.save(r);
        return ReceitaResponse.de(r);
    }

    public ReceitaResponse atualizar(Long id, ReceitaRequest request) {
        Receita r = buscarEntidade(id);
        if (!r.getSafra().getId().equals(request.getSafraId())) {
            r.setSafra(safraService.buscarEntidade(request.getSafraId()));
        }
        aplicar(r, request);
        receitaRepository.save(r);
        return ReceitaResponse.de(r);
    }

    public void deletar(Long id) {
        receitaRepository.delete(buscarEntidade(id));
    }

    private Receita buscarEntidade(Long id) {
        Receita r = receitaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receita nao encontrada"));
        safraService.buscarEntidade(r.getSafra().getId());
        return r;
    }

    private void aplicar(Receita r, ReceitaRequest request) {
        r.setDescricao(request.getDescricao());
        r.setValor(request.getValor());
        r.setDataRegistro(request.getDataRegistro());
    }

    // ----------------------------------------------------------------------
    // Sincronizacao (Fase 3). Mesmo padrao de SafraService: a safra-pai viaja
    // como "safraUuid"; se ainda nao chegou no servidor, o item e pulado e o
    // app tenta de novo no proximo "Sincronizar". @Transactional porque
    // ReceitaSyncResponse.de() le safra.getUuid() via proxy lazy.
    // ----------------------------------------------------------------------

    @Transactional
    public List<ReceitaSyncResponse> sincronizarPush(List<ReceitaSyncRequest> itens) {
        Usuario usuario = SecurityUtils.contaDados();
        List<ReceitaSyncResponse> respostas = new ArrayList<>();
        if (itens == null) return respostas;

        for (ReceitaSyncRequest item : itens) {
            if (item.getUuid() == null || item.getSafraUuid() == null
                    || item.getDescricao() == null || item.getDescricao().isBlank()
                    || item.getValor() == null) continue;

            LocalDate dataRegistro = converterData(item.getDataRegistro());
            if (dataRegistro == null) continue; // obrigatoria no banco

            Instant atualizadoCliente;
            try {
                atualizadoCliente = Instant.parse(item.getAtualizadoEm());
            } catch (Exception e) {
                continue;
            }

            Safra safra = safraService.buscarEntidadePorUuid(item.getSafraUuid());
            if (safra == null) continue; // safra-pai ainda nao sincronizou

            Receita r = receitaRepository.findByUuidAndSafra_Talhao_Propriedade_Usuario_Id(item.getUuid(), usuario.getId()).orElse(null);
            if (r == null) {
                r = new Receita();
                r.setUuid(item.getUuid());
                aplicarSync(r, item, safra, dataRegistro);
                receitaRepository.save(r);
            } else if (atualizadoCliente.isAfter(r.getAtualizadoEm())) {
                aplicarSync(r, item, safra, dataRegistro);
                receitaRepository.save(r);
            }
            respostas.add(ReceitaSyncResponse.de(r));
        }
        return respostas;
    }

    @Transactional(readOnly = true)
    public List<ReceitaSyncResponse> sincronizarPull(String desde) {
        Usuario usuario = SecurityUtils.contaDados();
        List<Receita> lista;
        if (desde == null || desde.isBlank()) {
            lista = receitaRepository.findBySafra_Talhao_Propriedade_Usuario_Id(usuario.getId());
        } else {
            Instant instanteDesde;
            try {
                instanteDesde = Instant.parse(desde);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parametro 'desde' invalido");
            }
            lista = receitaRepository.findBySafra_Talhao_Propriedade_Usuario_IdAndAtualizadoEmAfter(usuario.getId(), instanteDesde);
        }
        return lista.stream().map(ReceitaSyncResponse::de).toList();
    }

    private void aplicarSync(Receita r, ReceitaSyncRequest item, Safra safra, LocalDate dataRegistro) {
        r.setSafra(safra);
        r.setDescricao(item.getDescricao());
        r.setValor(BigDecimal.valueOf(item.getValor()));
        r.setDataRegistro(dataRegistro);
    }

    private LocalDate converterData(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try { return LocalDate.parse(iso); }
        catch (Exception e) { return null; }
    }
}
