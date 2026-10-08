package com.safrapay.backend.exclusao;

import com.safrapay.backend.entity.Usuario;
import com.safrapay.backend.exclusao.dto.ExclusaoSyncItem;
import com.safrapay.backend.security.SecurityUtils;
import com.safrapay.backend.util.DataSyncUtil;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Exclusões sincronizadas (V6, 08/10/2026).
 *
 * Antes, excluir no app apagava só no celular: o servidor continuava com o item e o pull
 * trazia ele de volta. Agora o app manda um aviso de exclusão (entidade + uuid); o servidor
 * marca {@code excluido_em} — sem apagar a linha — no item e, em cascata, nos filhos, do mesmo
 * jeito que o Room apaga no celular:
 * <ul>
 *   <li>propriedade → talhões → safras → despesas, receitas e metas;</li>
 *   <li>máquina → anotações; as despesas da máquina NÃO são excluídas, só desvinculadas
 *       (regra do Gustavo: excluir um "pai" nunca apaga lançamento financeiro);</li>
 *   <li>despesa, receita, meta e anotação: só o próprio item.</li>
 * </ul>
 * Item excluído some das listas e do pull normal e aparece em GET /api/exclusoes/sync, pros
 * outros aparelhos apagarem também. Push de um item excluído é ignorado (a exclusão vence).
 *
 * Tudo em SQL direto porque a cascata atravessa várias tabelas (as FKs do banco não têm
 * ON DELETE e a linha continua existindo).
 */
@Service
public class ExclusaoService {

    private static final int LIMITE_POR_ENVIO = 1000;
    private static final String SET = " set excluido_em = :t, atualizado_em = :t where excluido_em is null and ";
    private static final String[] LANCAMENTOS = {"despesa", "receita", "meta_financeira"};

    /** entidade do app → SQL que acha o id do item (da fazenda certa e ainda não excluído). */
    private static final Map<String, String> BUSCA = Map.of(
            "propriedade", "select x.id from propriedade x where x.uuid = :uuid and x.usuario_id = :dono and x.excluido_em is null",
            "talhao", "select x.id from talhao x join propriedade p on p.id = x.propriedade_id"
                    + " where x.uuid = :uuid and p.usuario_id = :dono and x.excluido_em is null",
            "safra", "select x.id from safra x join talhao t on t.id = x.talhao_id join propriedade p on p.id = t.propriedade_id"
                    + " where x.uuid = :uuid and p.usuario_id = :dono and x.excluido_em is null",
            "receita", buscaLancamento("receita"),
            "despesa", buscaLancamento("despesa"),
            "meta", buscaLancamento("meta_financeira"),
            "maquina", "select x.id from maquina x where x.uuid = :uuid and x.usuario_id = :dono and x.excluido_em is null",
            "anotacao_maquina", "select x.id from anotacao_maquina x join maquina m on m.id = x.maquina_id"
                    + " where x.uuid = :uuid and m.usuario_id = :dono and x.excluido_em is null");

    /** Avisos de exclusão de todas as tabelas, da fazenda do dono. */
    private static final String AVISOS = "select 'propriedade' as entidade, x.uuid, x.excluido_em from propriedade x"
            + " where x.usuario_id = :dono and x.excluido_em is not null"
            + " union all select 'talhao', x.uuid, x.excluido_em from talhao x join propriedade p on p.id = x.propriedade_id"
            + " where p.usuario_id = :dono and x.excluido_em is not null"
            + " union all select 'safra', x.uuid, x.excluido_em from safra x join talhao t on t.id = x.talhao_id"
            + " join propriedade p on p.id = t.propriedade_id where p.usuario_id = :dono and x.excluido_em is not null"
            + avisoLancamento("receita", "receita") + avisoLancamento("despesa", "despesa") + avisoLancamento("meta", "meta_financeira")
            + " union all select 'maquina', x.uuid, x.excluido_em from maquina x where x.usuario_id = :dono and x.excluido_em is not null"
            + " union all select 'anotacao_maquina', x.uuid, x.excluido_em from anotacao_maquina x join maquina m on m.id = x.maquina_id"
            + " where m.usuario_id = :dono and x.excluido_em is not null";

    private final NamedParameterJdbcTemplate jdbc;

    public ExclusaoService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** POST /api/exclusoes/sync — aplica os avisos do app. Devolve quantos itens foram excluídos agora. */
    @Transactional
    public int receber(List<ExclusaoSyncItem> itens) {
        if (itens == null || itens.isEmpty()) return 0;
        if (itens.size() > LIMITE_POR_ENVIO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie no máximo " + LIMITE_POR_ENVIO + " exclusões por vez");
        }
        Usuario dono = SecurityUtils.contaDados();
        // Hora do servidor (não a do celular), em milissegundos: é ela que os outros aparelhos
        // usam como marcador "desde" pra buscar os avisos novos.
        Instant agora = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        int feitas = 0;
        for (ExclusaoSyncItem item : itens) {
            if (item == null || item.getUuid() == null || item.getEntidade() == null) continue;
            String entidade = item.getEntidade().trim().toLowerCase();
            String busca = BUSCA.get(entidade);
            if (busca == null) continue; // entidade desconhecida
            List<Long> ids = jdbc.queryForList(busca,
                    new MapSqlParameterSource("uuid", item.getUuid()).addValue("dono", dono.getId()), Long.class);
            if (ids.isEmpty()) continue; // não existe, é de outra fazenda ou já foi excluído
            excluirEmCascata(entidade, ids.get(0), agora);
            feitas++;
        }
        return feitas;
    }

    /** GET /api/exclusoes/sync?desde= — avisos de exclusão da fazenda depois de "desde" (vazio = todos). */
    @Transactional(readOnly = true)
    public List<ExclusaoSyncItem> enviar(String desde) {
        Usuario dono = SecurityUtils.contaDados();
        MapSqlParameterSource p = new MapSqlParameterSource("dono", dono.getId());
        String sql = "select e.entidade, e.uuid, e.excluido_em from (" + AVISOS + ") e";
        if (desde != null && !desde.isBlank()) {
            Instant instante;
            try {
                instante = Instant.parse(desde);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parametro 'desde' invalido");
            }
            sql += " where e.excluido_em > :desde";
            p.addValue("desde", OffsetDateTime.ofInstant(instante, ZoneOffset.UTC));
        }
        sql += " order by e.excluido_em";
        return jdbc.query(sql, p, (rs, i) -> new ExclusaoSyncItem(
                rs.getString("entidade"),
                rs.getString("uuid"),
                DataSyncUtil.formatar(rs.getObject("excluido_em", OffsetDateTime.class).toInstant())));
    }

    // ------------------------------------------------------------------

    private void excluirEmCascata(String entidade, long id, Instant agora) {
        MapSqlParameterSource p = new MapSqlParameterSource("id", id)
                .addValue("t", OffsetDateTime.ofInstant(agora, ZoneOffset.UTC));
        switch (entidade) {
            case "propriedade" -> {
                for (String l : LANCAMENTOS) {
                    jdbc.update("update " + l + SET + "safra_id in (select s.id from safra s join talhao t on t.id = s.talhao_id"
                            + " where t.propriedade_id = :id)", p);
                }
                jdbc.update("update safra" + SET + "talhao_id in (select id from talhao where propriedade_id = :id)", p);
                jdbc.update("update talhao" + SET + "propriedade_id = :id", p);
                jdbc.update("update propriedade" + SET + "id = :id", p);
            }
            case "talhao" -> {
                for (String l : LANCAMENTOS) {
                    jdbc.update("update " + l + SET + "safra_id in (select id from safra where talhao_id = :id)", p);
                }
                jdbc.update("update safra" + SET + "talhao_id = :id", p);
                jdbc.update("update talhao" + SET + "id = :id", p);
            }
            case "safra" -> {
                for (String l : LANCAMENTOS) jdbc.update("update " + l + SET + "safra_id = :id", p);
                jdbc.update("update safra" + SET + "id = :id", p);
            }
            case "maquina" -> {
                // Despesas ficam, só perdem o vínculo (os outros aparelhos recebem no pull de despesas).
                jdbc.update("update despesa set maquina_id = null, atualizado_em = :t where maquina_id = :id", p);
                jdbc.update("update anotacao_maquina" + SET + "maquina_id = :id", p);
                jdbc.update("update maquina" + SET + "id = :id", p);
            }
            case "receita", "despesa", "anotacao_maquina" -> jdbc.update("update " + entidade + SET + "id = :id", p);
            case "meta" -> jdbc.update("update meta_financeira" + SET + "id = :id", p);
            default -> { }
        }
    }

    private static String buscaLancamento(String tabela) {
        return "select x.id from " + tabela + " x join safra s on s.id = x.safra_id join talhao t on t.id = s.talhao_id"
                + " join propriedade p on p.id = t.propriedade_id where x.uuid = :uuid and p.usuario_id = :dono and x.excluido_em is null";
    }

    private static String avisoLancamento(String entidade, String tabela) {
        return " union all select '" + entidade + "', x.uuid, x.excluido_em from " + tabela + " x join safra s on s.id = x.safra_id"
                + " join talhao t on t.id = s.talhao_id join propriedade p on p.id = t.propriedade_id"
                + " where p.usuario_id = :dono and x.excluido_em is not null";
    }
}
