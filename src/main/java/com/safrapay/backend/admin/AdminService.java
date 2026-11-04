package com.safrapay.backend.admin;

import com.safrapay.backend.admin.dto.AdminUsuarioResponse;
import com.safrapay.backend.admin.dto.AdminUsuariosResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Lista de contas pro admin. Nunca devolve senha (nem o hash), CPF, telefone
 * ou nascimento: só o necessário pra saber quem usa o app e quanto.
 */
@Service
public class AdminService {

    /**
     * Uma linha por conta. Contagens ignoram itens excluídos (excluido_em, V6).
     * Receitas/despesas contam pela propriedade (receita -> safra -> talhão ->
     * propriedade). "Último acesso" = último login ou renovação de token
     * (refresh_token.data_criacao): o app renova no máximo a cada 24 h de uso.
     */
    private static final String SQL = """
            select u.id, u.nome, u.email, u.email_verificado, u.criado_em,
                   em.papel as papel_equipe,
                   d.email as dono_email,
                   (select count(*) from equipe_membro e
                     where e.dono_usuario_id = u.id and e.status = 'ACEITO') as membros,
                   (select count(*) from propriedade p
                     where p.usuario_id = u.id and p.excluido_em is null) as propriedades,
                   (select count(*) from receita r
                      join safra s on s.id = r.safra_id
                      join talhao t on t.id = s.talhao_id
                      join propriedade p on p.id = t.propriedade_id
                     where p.usuario_id = u.id and r.excluido_em is null) as receitas,
                   (select count(*) from despesa x
                      join safra s on s.id = x.safra_id
                      join talhao t on t.id = s.talhao_id
                      join propriedade p on p.id = t.propriedade_id
                     where p.usuario_id = u.id and x.excluido_em is null) as despesas,
                   (select max(rt.data_criacao) from refresh_token rt
                     where rt.usuario_id = u.id) as ultimo_acesso
              from usuario u
              left join equipe_membro em on em.membro_usuario_id = u.id and em.status = 'ACEITO'
              left join usuario d on d.id = em.dono_usuario_id
             order by u.criado_em desc
            """;

    private final JdbcTemplate jdbc;

    public AdminService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public AdminUsuariosResponse listarUsuarios() {
        List<AdminUsuarioResponse> usuarios = jdbc.query(SQL, (rs, i) -> linha(rs));
        long verificados = usuarios.stream().filter(AdminUsuarioResponse::emailVerificado).count();
        return new AdminUsuariosResponse(usuarios.size(), verificados, Instant.now().toString(), usuarios);
    }

    private static AdminUsuarioResponse linha(ResultSet rs) throws SQLException {
        String papelEquipe = rs.getString("papel_equipe");
        return new AdminUsuarioResponse(
                rs.getLong("id"),
                rs.getString("nome"),
                rs.getString("email"),
                rs.getBoolean("email_verificado"),
                iso(rs.getTimestamp("criado_em")),
                papelEquipe == null ? "DONO" : papelEquipe,
                rs.getString("dono_email"),
                rs.getLong("membros"),
                rs.getLong("propriedades"),
                rs.getLong("receitas"),
                rs.getLong("despesas"),
                iso(rs.getTimestamp("ultimo_acesso")));
    }

    private static String iso(Timestamp t) {
        return t == null ? null : t.toInstant().toString();
    }
}
