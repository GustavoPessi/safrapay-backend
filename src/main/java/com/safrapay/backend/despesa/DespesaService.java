package com.safrapay.backend.despesa;

import com.safrapay.backend.util.DataSyncUtil;
import com.safrapay.backend.despesa.dto.DespesaRequest;
import com.safrapay.backend.despesa.dto.DespesaResponse;
import com.safrapay.backend.despesa.dto.DespesaSyncRequest;
import com.safrapay.backend.despesa.dto.DespesaSyncResponse;
import com.safrapay.backend.entity.CategoriaDespesa;
import com.safrapay.backend.entity.Despesa;
import com.safrapay.backend.entity.Maquina;
import com.safrapay.backend.entity.Safra;
import com.safrapay.backend.entity.Usuario;
import com.safrapay.backend.maquina.MaquinaService;
import com.safrapay.backend.repository.CategoriaDespesaRepository;
import com.safrapay.backend.repository.DespesaRepository;
import com.safrapay.backend.repository.MaquinaRepository;
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
public class DespesaService {

    private final DespesaRepository despesaRepository;
    private final CategoriaDespesaRepository categoriaDespesaRepository;
    private final SafraService safraService;
    private final MaquinaService maquinaService;
    private final MaquinaRepository maquinaRepository;

    public DespesaService(DespesaRepository despesaRepository,
                           CategoriaDespesaRepository categoriaDespesaRepository,
                           SafraService safraService,
                           MaquinaService maquinaService,
                           MaquinaRepository maquinaRepository) {
        this.despesaRepository = despesaRepository;
        this.categoriaDespesaRepository = categoriaDespesaRepository;
        this.safraService = safraService;
        this.maquinaService = maquinaService;
        this.maquinaRepository = maquinaRepository;
    }

    public List<DespesaResponse> listarPorSafra(Long safraId) {
        safraService.buscarEntidade(safraId);
        return despesaRepository.findBySafraId(safraId).stream().map(DespesaResponse::de).toList();
    }

    public DespesaResponse buscar(Long id) {
        return DespesaResponse.de(buscarEntidade(id));
    }

    public DespesaResponse criar(DespesaRequest request) {
        Despesa d = new Despesa();
        preencherRelacionamentos(d, request);
        aplicar(d, request);
        despesaRepository.save(d);
        return DespesaResponse.de(d);
    }

    public DespesaResponse atualizar(Long id, DespesaRequest request) {
        Despesa d = buscarEntidade(id);
        preencherRelacionamentos(d, request);
        aplicar(d, request);
        despesaRepository.save(d);
        return DespesaResponse.de(d);
    }

    public void deletar(Long id) {
        despesaRepository.delete(buscarEntidade(id));
    }

    private Despesa buscarEntidade(Long id) {
        Despesa d = despesaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Despesa nao encontrada"));
        safraService.buscarEntidade(d.getSafra().getId());
        return d;
    }

    private void preencherRelacionamentos(Despesa d, DespesaRequest request) {
        Safra safra = safraService.buscarEntidade(request.getSafraId());
        CategoriaDespesa categoria = categoriaDespesaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria nao encontrada"));
        d.setSafra(safra);
        d.setCategoria(categoria);

        if (request.getMaquinaId() != null) {
            Maquina maquina = maquinaService.buscarEntidade(request.getMaquinaId());
            d.setMaquina(maquina);
        } else {
            d.setMaquina(null);
        }
    }

    private void aplicar(Despesa d, DespesaRequest request) {
        d.setDescricao(request.getDescricao());
        d.setValor(request.getValor());
        d.setDataRegistro(request.getDataRegistro());
    }

    // ----------------------------------------------------------------------
    // Sincronizacao (Fase 3). Mesmo padrao de ReceitaService, com dois
    // relacionamentos a mais:
    // - Categoria: resolvida pelo NOME. Se ainda nao existir no servidor (o
    //   backend nao tem seed dessa tabela), e criada aqui na hora - a tabela e
    //   um catalogo global de nomes fixos, igual ao seed do app.
    // - Maquina (opcional): resolvida pelo uuid, escopada ao usuario. Desde a
    //   Fase 4 o app sincroniza as maquinas ANTES das despesas, entao a maquina
    //   normalmente ja existe aqui. Se mesmo assim vier uma maquina que o
    //   servidor nao conhece, o vinculo existente no servidor e mantido e nada
    //   e apagado; o app, do lado dele, nunca apaga o vinculo local por causa
    //   de um maquinaUuid nulo vindo do servidor.
    // @Transactional porque DespesaSyncResponse.de() le safra/categoria/maquina
    // via proxy lazy.
    // ----------------------------------------------------------------------

    @Transactional
    public List<DespesaSyncResponse> sincronizarPush(List<DespesaSyncRequest> itens) {
        Usuario usuario = SecurityUtils.contaDados();
        List<DespesaSyncResponse> respostas = new ArrayList<>();
        if (itens == null) return respostas;

        for (DespesaSyncRequest item : itens) {
            if (item.getUuid() == null || item.getSafraUuid() == null
                    || item.getCategoriaNome() == null || item.getCategoriaNome().isBlank()
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

            CategoriaDespesa categoria = resolverCategoria(item.getCategoriaNome());

            Despesa d = despesaRepository.findByUuidAndSafra_Talhao_Propriedade_Usuario_Id(item.getUuid(), usuario.getId()).orElse(null);
            // Excluído (V6): a exclusão vence; não ressuscita nem devolve o item.
            if (d != null && d.getExcluidoEm() != null) continue;
            if (d == null) {
                d = new Despesa();
                d.setUuid(item.getUuid());
                aplicarSync(d, item, safra, categoria, dataRegistro, usuario);
                despesaRepository.save(d);
            } else if (atualizadoCliente.isAfter(d.getAtualizadoEm())) {
                aplicarSync(d, item, safra, categoria, dataRegistro, usuario);
                despesaRepository.save(d);
            }
            respostas.add(DespesaSyncResponse.de(d));
        }
        return respostas;
    }

    @Transactional(readOnly = true)
    public List<DespesaSyncResponse> sincronizarPull(String desde) {
        Usuario usuario = SecurityUtils.contaDados();
        List<Despesa> lista;
        if (desde == null || desde.isBlank()) {
            lista = despesaRepository.findBySafra_Talhao_Propriedade_Usuario_Id(usuario.getId());
        } else {
            Instant instanteDesde;
            try {
                instanteDesde = DataSyncUtil.depoisDoMarcador(desde);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parametro 'desde' invalido");
            }
            lista = despesaRepository.findBySafra_Talhao_Propriedade_Usuario_IdAndAtualizadoEmAfter(usuario.getId(), instanteDesde);
        }
        return lista.stream().map(DespesaSyncResponse::de).toList();
    }

    private void aplicarSync(Despesa d, DespesaSyncRequest item, Safra safra, CategoriaDespesa categoria,
                             LocalDate dataRegistro, Usuario usuario) {
        d.setSafra(safra);
        d.setCategoria(categoria);
        d.setDescricao(item.getDescricao());
        d.setValor(BigDecimal.valueOf(item.getValor()));
        d.setDataRegistro(dataRegistro);
        if (item.getMaquinaUuid() == null) {
            d.setMaquina(null); // o app removeu o vinculo com a maquina
        } else {
            Maquina maquina = maquinaRepository.findByUuidAndUsuarioId(item.getMaquinaUuid(), usuario.getId())
                    .filter(x -> x.getExcluidoEm() == null).orElse(null);
            if (maquina != null) d.setMaquina(maquina);
            // maquina desconhecida (ainda nao sincronizada): mantem o que ja havia
        }
    }

    private CategoriaDespesa resolverCategoria(String nome) {
        String n = nome.trim();
        return categoriaDespesaRepository.findByNome(n).orElseGet(() -> {
            CategoriaDespesa c = new CategoriaDespesa();
            c.setNome(n);
            return categoriaDespesaRepository.save(c);
        });
    }

    private LocalDate converterData(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try { return LocalDate.parse(iso); }
        catch (Exception e) { return null; }
    }
}
