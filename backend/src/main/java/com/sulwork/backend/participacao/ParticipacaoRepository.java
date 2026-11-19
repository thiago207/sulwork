package com.sulwork.backend.participacao;

import com.sulwork.backend.participante.Participante;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ParticipacaoRepository extends JpaRepository<Participacao, Long> {

    Optional<Participacao> findByEventoIdAndParticipanteId(Long eventoId, Long participanteId);

    @Query("""
            select p.participante from Participacao p
            where p.evento.id = :eventoId and p.ativo = true and p.participante.ativo = true
            order by p.participante.nome""")
    List<Participante> findParticipantesAtivos(Long eventoId);
}
