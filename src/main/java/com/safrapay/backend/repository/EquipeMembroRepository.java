package com.safrapay.backend.repository;

import com.safrapay.backend.entity.EquipeMembro;
import com.safrapay.backend.entity.StatusMembro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EquipeMembroRepository extends JpaRepository<EquipeMembro, Long> {

    Optional<EquipeMembro> findByUuid(String uuid);

    /** Participação aceita da pessoa (no máximo uma — índice único no banco). */
    @Query("select e from EquipeMembro e join fetch e.dono where e.membro.id = :membroId and e.status = com.safrapay.backend.entity.StatusMembro.ACEITO")
    Optional<EquipeMembro> buscarParticipacaoAceita(@Param("membroId") Long membroId);

    /** Membros e convites da equipe de um dono, nos status pedidos, mais recentes primeiro. */
    @Query("select e from EquipeMembro e left join fetch e.membro where e.dono.id = :donoId and e.status in :status order by e.criadoEm desc")
    List<EquipeMembro> listarDaEquipe(@Param("donoId") Long donoId, @Param("status") Collection<StatusMembro> status);

    /** Convites pendentes para a pessoa: pelo vínculo de conta ou pelo e-mail (convite feito antes do cadastro). */
    @Query("select e from EquipeMembro e join fetch e.dono left join e.membro m where e.status = com.safrapay.backend.entity.StatusMembro.PENDENTE "
            + "and (m.id = :usuarioId or lower(e.email) = lower(:email)) order by e.criadoEm desc")
    List<EquipeMembro> convitesPendentesPara(@Param("usuarioId") Long usuarioId, @Param("email") String email);

    /** Convite ativo (pendente/aceito) de um dono para um e-mail. */
    @Query("select e from EquipeMembro e where e.dono.id = :donoId and lower(e.email) = lower(:email) and e.status in (com.safrapay.backend.entity.StatusMembro.PENDENTE, com.safrapay.backend.entity.StatusMembro.ACEITO)")
    Optional<EquipeMembro> buscarAtivo(@Param("donoId") Long donoId, @Param("email") String email);

    /** Convites ativos (pendentes + aceitos) do dono — limite de tamanho da equipe. */
    @Query("select count(e) from EquipeMembro e where e.dono.id = :donoId and e.status in (com.safrapay.backend.entity.StatusMembro.PENDENTE, com.safrapay.backend.entity.StatusMembro.ACEITO)")
    long contarAtivosDoDono(@Param("donoId") Long donoId);

    /** Membros aceitos do dono — quem tem membros não pode entrar em outra equipe. */
    @Query("select count(e) from EquipeMembro e where e.dono.id = :donoId and e.status = com.safrapay.backend.entity.StatusMembro.ACEITO")
    long contarAceitosDoDono(@Param("donoId") Long donoId);
}
