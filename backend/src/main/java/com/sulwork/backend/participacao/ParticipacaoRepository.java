package com.sulwork.backend.participacao;

import com.sulwork.backend.evento.StatusEvento;
import com.sulwork.backend.participante.Participante;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ParticipacaoRepository extends JpaRepository<Participacao, Long> {

    Optional<Participacao> findByEventoIdAndParticipanteId(Long eventoId, Long participanteId);

    @Query("""
            select p.participante from Participacao p
            where p.evento.id = :eventoId and p.ativo = true and p.participante.ativo = true
            order by p.participante.nome""")
    List<Participante> findParticipantesAtivos(Long eventoId);

    @Query("""
            select p.evento.nome from Participacao p
            where p.participante.id = :participanteId and p.ativo = true and p.evento.status in :status
            order by p.evento.dataHora""")
    List<String> findNomesDosEventos(Long participanteId, Collection<StatusEvento> status);

    @Modifying
    @Query("update Participacao p set p.ativo = false where p.participante.id = :participanteId and p.ativo = true")
    int desativarDoParticipante(Long participanteId);
}
