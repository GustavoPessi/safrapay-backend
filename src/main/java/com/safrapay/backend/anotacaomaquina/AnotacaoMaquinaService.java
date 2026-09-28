package com.safrapay.backend.anotacaomaquina;

import com.safrapay.backend.anotacaomaquina.dto.AnotacaoMaquinaRequest;
import com.safrapay.backend.anotacaomaquina.dto.AnotacaoMaquinaResponse;
import com.safrapay.backend.anotacaomaquina.dto.AnotacaoMaquinaSyncRequest;
import com.safrapay.backend.anotacaomaquina.dto.AnotacaoMaquinaSyncResponse;
import com.safrapay.backend.entity.AnotacaoMaquina;
import com.safrapay.backend.entity.Maquina;
import com.safrapay.backend.entity.Usuario;
import com.safrapay.backend.maquina.MaquinaService;
import com.safrapay.backend.repository.AnotacaoMaquinaRepository;
import com.safrapay.backend.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
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
        a.setConcluida(Boolean.TRUE.equals(request.getConcluida()));
    }

    // ----------------------------------------------------------------------
    // Sincronizacao (Fase 4, 28/09/2026). Mesmo padrao de TalhaoService: a
    // maquina-pai viaja como "maquinaUuid" e eh resolvida via
    // MaquinaService.buscarEntidadePorUuid (escopada ao usuario logado). Se a
    // maquina ainda nao chegou no servidor, o item eh pulado nesta passada -
    // regra: maquina sincroniza sempre antes da anotacao.
    // @Transactional porque AnotacaoMaquinaSyncResponse.de() le
    // a.getMaquina().getUuid() via proxy lazy (open-in-view=false).
    // ----------------------------------------------------------------------

    @Transactional
    public List<AnotacaoMaquinaSyncResponse> sincronizarPush(List<AnotacaoMaquinaSyncRequest> itens) {
        Usuario usuario = SecurityUtils.usuarioLogado();
        List<AnotacaoMaquinaSyncResponse> respostas = new ArrayList<>();
        if (itens == null) return respostas;

        for (AnotacaoMaquinaSyncRequest item : itens) {
            if (item.getUuid() == null || item.getMaquinaUuid() == null
                    || item.getTexto() == null || item.getTexto().isBlank()) continue;

            Instant atualizadoCliente;
            try {
                atualizadoCliente = Instant.parse(item.getAtualizadoEm());
            } catch (Exception e) {
                continue;
            }

            Maquina maquina = maquinaService.buscarEntidadePorUuid(item.getMaquinaUuid());
            if (maquina == null) continue; // maquina-pai ainda nao sincronizou

            AnotacaoMaquina a = repository.findByUuidAndMaquina_Usuario_Id(item.getUuid(), usuario.getId()).orElse(null);
            if (a == null) {
                a = new AnotacaoMaquina();
                a.setUuid(item.getUuid());
                aplicarSync(a, item, maquina);
                repository.save(a);
            } else if (atualizadoCliente.isAfter(a.getAtualizadoEm())) {
                aplicarSync(a, item, maquina);
                repository.save(a);
            }
            respostas.add(AnotacaoMaquinaSyncResponse.de(a));
        }
        return respostas;
    }

    @Transactional(readOnly = true)
    public List<AnotacaoMaquinaSyncResponse> sincronizarPull(String desde) {
        Usuario usuario = SecurityUtils.usuarioLogado();
        List<AnotacaoMaquina> lista;
        if (desde == null || desde.isBlank()) {
            lista = repository.findByMaquina_Usuario_Id(usuario.getId());
        } else {
            Instant instanteDesde;
            try {
                instanteDesde = Instant.parse(desde);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parametro 'desde' invalido");
            }
            lista = repository.findByMaquina_Usuario_IdAndAtualizadoEmAfter(usuario.getId(), instanteDesde);
        }
        return lista.stream().map(AnotacaoMaquinaSyncResponse::de).toList();
    }

    private void aplicarSync(AnotacaoMaquina a, AnotacaoMaquinaSyncRequest item, Maquina maquina) {
        a.setMaquina(maquina);
        a.setTexto(item.getTexto());
        a.setData(parseData(item.getData()));
        a.setConcluida(Boolean.TRUE.equals(item.getConcluida()));
    }

    private LocalDate parseData(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try { return LocalDate.parse(iso); }
        catch (Exception e) { return null; }
    }
}
