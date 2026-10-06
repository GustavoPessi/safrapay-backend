package com.safrapay.backend.propriedade;

import com.safrapay.backend.entity.Propriedade;
import com.safrapay.backend.entity.Usuario;
import com.safrapay.backend.propriedade.dto.PropriedadeRequest;
import com.safrapay.backend.propriedade.dto.PropriedadeResponse;
import com.safrapay.backend.propriedade.dto.PropriedadeSyncRequest;
import com.safrapay.backend.propriedade.dto.PropriedadeSyncResponse;
import com.safrapay.backend.repository.PropriedadeRepository;
import com.safrapay.backend.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class PropriedadeService {

    private final PropriedadeRepository propriedadeRepository;

    public PropriedadeService(PropriedadeRepository propriedadeRepository) {
        this.propriedadeRepository = propriedadeRepository;
    }

    public List<PropriedadeResponse> listar() {
        Usuario usuario = SecurityUtils.contaDados();
        return propriedadeRepository.findByUsuarioId(usuario.getId()).stream()
                .map(PropriedadeResponse::de)
                .toList();
    }

    public PropriedadeResponse buscar(Long id) {
        return PropriedadeResponse.de(buscarEntidade(id));
    }

    public PropriedadeResponse criar(PropriedadeRequest request) {
        Propriedade p = new Propriedade();
        p.setUsuario(SecurityUtils.contaDados());
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

        if (!p.getUsuario().getId().equals(SecurityUtils.contaDados().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Propriedade nao encontrada");
        }
        return p;
    }

    /**
     * Busca a propriedade pelo uuid (identidade estavel usada na sincronizacao),
     * escopada ao usuario logado. Retorna null se nao existir ainda no servidor -
     * usado pelo TalhaoService pra resolver o pai antes de gravar um talhao.
     */
    public Propriedade buscarEntidadePorUuid(String uuid) {
        Usuario usuario = SecurityUtils.contaDados();
        return propriedadeRepository.findByUuidAndUsuarioId(uuid, usuario.getId()).orElse(null);
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
        p.setLatitude(request.getLatitude());
        p.setLongitude(request.getLongitude());
    }

    // ----------------------------------------------------------------------
    // Sincronizacao (push/pull) - "todas as informacoes do app tem que ter
    // conexao com o banco" (14/09/2026). Ver TalhaoService pra o mesmo padrao
    // aplicado numa entidade filha.
    // ----------------------------------------------------------------------

    /**
     * Recebe o lote de propriedades alteradas localmente e aplica "last write
     * wins": so sobrescreve o que ja existe no servidor se o horario da
     * alteracao no celular for mais recente que o horario salvo no servidor.
     * Devolve o estado final de cada item (aceito ou nao) pra o app reconciliar.
     */
    public List<PropriedadeSyncResponse> sincronizarPush(List<PropriedadeSyncRequest> itens) {
        Usuario usuario = SecurityUtils.contaDados();
        List<PropriedadeSyncResponse> respostas = new ArrayList<>();
        if (itens == null) return respostas;

        for (PropriedadeSyncRequest item : itens) {
            if (item.getUuid() == null || item.getNome() == null || item.getNome().isBlank()) continue;

            Instant atualizadoCliente;
            try {
                atualizadoCliente = Instant.parse(item.getAtualizadoEm());
            } catch (Exception e) {
                continue; // item sem timestamp valido - ignora, o app tenta de novo na proxima sincronizacao
            }

            Propriedade p = propriedadeRepository.findByUuidAndUsuarioId(item.getUuid(), usuario.getId()).orElse(null);
            if (p == null) {
                p = new Propriedade();
                p.setUuid(item.getUuid());
                p.setUsuario(usuario);
                aplicarSync(p, item);
                propriedadeRepository.save(p);
            } else if (atualizadoCliente.isAfter(p.getAtualizadoEm())) {
                aplicarSync(p, item);
                propriedadeRepository.save(p);
            }
            // se o servidor ja tem algo mais novo, ignora o item do cliente e devolve
            // o estado atual do servidor mesmo assim, pra o app se realinhar.
            respostas.add(PropriedadeSyncResponse.de(p));
        }
        return respostas;
    }

    /** Devolve as propriedades do usuario logado alteradas depois de "desde" (ou todas, se vazio). */
    public List<PropriedadeSyncResponse> sincronizarPull(String desde) {
        Usuario usuario = SecurityUtils.contaDados();
        List<Propriedade> lista;
        if (desde == null || desde.isBlank()) {
            lista = propriedadeRepository.findByUsuarioId(usuario.getId());
        } else {
            Instant instanteDesde;
            try {
                instanteDesde = Instant.parse(desde);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parametro 'desde' invalido");
            }
            lista = propriedadeRepository.findByUsuarioIdAndAtualizadoEmAfter(usuario.getId(), instanteDesde);
        }
        return lista.stream().map(PropriedadeSyncResponse::de).toList();
    }

    private void aplicarSync(Propriedade p, PropriedadeSyncRequest item) {
        p.setNome(item.getNome());
        p.setLocalidade(item.getLocalidade());
        p.setAreaTotal(item.getAreaTotal());
        p.setTipoSolo(item.getTipoSolo());
        p.setMatricula(item.getMatricula());
        p.setCib(item.getCib());
        p.setValor(item.getValor());
        p.setObservacoes(item.getObservacoes());
        p.setLatitude(item.getLatitude());
        p.setLongitude(item.getLongitude());
    }
}
