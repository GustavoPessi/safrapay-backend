package com.safrapay.backend.maquina;

import com.safrapay.backend.util.DataSyncUtil;
import com.safrapay.backend.entity.Maquina;
import com.safrapay.backend.entity.Usuario;
import com.safrapay.backend.maquina.dto.MaquinaRequest;
import com.safrapay.backend.maquina.dto.MaquinaResponse;
import com.safrapay.backend.maquina.dto.MaquinaSyncRequest;
import com.safrapay.backend.maquina.dto.MaquinaSyncResponse;
import com.safrapay.backend.repository.MaquinaRepository;
import com.safrapay.backend.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class MaquinaService {

    private final MaquinaRepository maquinaRepository;

    public MaquinaService(MaquinaRepository maquinaRepository) {
        this.maquinaRepository = maquinaRepository;
    }

    public List<MaquinaResponse> listar() {
        Usuario usuario = SecurityUtils.contaDados();
        return maquinaRepository.findByUsuarioId(usuario.getId()).stream()
                .map(MaquinaResponse::de)
                .toList();
    }

    public MaquinaResponse buscar(Long id) {
        return MaquinaResponse.de(buscarEntidade(id));
    }

    public MaquinaResponse criar(MaquinaRequest request) {
        Maquina m = new Maquina();
        m.setUsuario(SecurityUtils.contaDados());
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

        if (!m.getUsuario().getId().equals(SecurityUtils.contaDados().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Maquina nao encontrada");
        }
        return m;
    }

    private void aplicar(Maquina m, MaquinaRequest request) {
        m.setNome(request.getNome());
        m.setMarca(request.getMarca());
        m.setTipo(request.getTipo());
        m.setModelo(request.getModelo());
        m.setAno(request.getAno());
        m.setIdentificacao(request.getIdentificacao());
        m.setValor(request.getValor());
        m.setObservacoes(request.getObservacoes());
    }

    // ----------------------------------------------------------------------
    // Sincronizacao (Fase 4, 28/09/2026). Mesmo padrao de PropriedadeService:
    // Maquina pertence direto ao usuario, entao nao tem pai pra resolver.
    // Upsert por uuid escopado ao usuario logado + "last write wins" por
    // atualizadoEm. MaquinaSyncResponse nao toca relacao lazy, mas os metodos
    // ficam @Transactional por consistencia com as outras entidades.
    // ----------------------------------------------------------------------

    /** Usado pela sincronizacao de AnotacaoMaquina pra resolver a maquina-pai. */
    public Maquina buscarEntidadePorUuid(String uuid) {
        Usuario usuario = SecurityUtils.contaDados();
        return maquinaRepository.findByUuidAndUsuarioId(uuid, usuario.getId())
                .filter(x -> x.getExcluidoEm() == null) // pai excluído = como se não existisse
                .orElse(null);
    }

    @Transactional
    public List<MaquinaSyncResponse> sincronizarPush(List<MaquinaSyncRequest> itens) {
        Usuario usuario = SecurityUtils.contaDados();
        List<MaquinaSyncResponse> respostas = new ArrayList<>();
        if (itens == null) return respostas;

        for (MaquinaSyncRequest item : itens) {
            if (item.getUuid() == null || item.getNome() == null || item.getNome().isBlank()) continue;

            Instant atualizadoCliente;
            try {
                atualizadoCliente = Instant.parse(item.getAtualizadoEm());
            } catch (Exception e) {
                continue; // sem timestamp valido - o app tenta de novo na proxima sincronizacao
            }

            Maquina m = maquinaRepository.findByUuidAndUsuarioId(item.getUuid(), usuario.getId()).orElse(null);
            // Excluído (V6): a exclusão vence; não ressuscita nem devolve o item.
            if (m != null && m.getExcluidoEm() != null) continue;
            if (m == null) {
                m = new Maquina();
                m.setUuid(item.getUuid());
                m.setUsuario(usuario);
                aplicarSync(m, item);
                maquinaRepository.save(m);
            } else if (atualizadoCliente.isAfter(m.getAtualizadoEm())) {
                aplicarSync(m, item);
                maquinaRepository.save(m);
            }
            respostas.add(MaquinaSyncResponse.de(m));
        }
        return respostas;
    }

    @Transactional(readOnly = true)
    public List<MaquinaSyncResponse> sincronizarPull(String desde) {
        Usuario usuario = SecurityUtils.contaDados();
        List<Maquina> lista;
        if (desde == null || desde.isBlank()) {
            lista = maquinaRepository.findByUsuarioId(usuario.getId());
        } else {
            Instant instanteDesde;
            try {
                instanteDesde = DataSyncUtil.depoisDoMarcador(desde);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parametro 'desde' invalido");
            }
            lista = maquinaRepository.findByUsuarioIdAndAtualizadoEmAfter(usuario.getId(), instanteDesde);
        }
        return lista.stream().map(MaquinaSyncResponse::de).toList();
    }

    private void aplicarSync(Maquina m, MaquinaSyncRequest item) {
        m.setNome(item.getNome());
        m.setMarca(item.getMarca());
        m.setTipo(item.getTipo());
        m.setModelo(item.getModelo());
        m.setAno(item.getAno());
        m.setIdentificacao(item.getIdentificacao());
        m.setValor(item.getValor());
        m.setObservacoes(item.getObservacoes());
    }
}
