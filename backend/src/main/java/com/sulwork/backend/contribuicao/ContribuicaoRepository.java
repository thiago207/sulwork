package com.sulwork.backend.contribuicao;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ContribuicaoRepository extends JpaRepository<Contribuicao, Long> {

    @Query(value = """
            select coalesce(sum(c.quantidade), 0) from contribuicao c
            join participacao p on p.id = c.participacao_id
            where c.item_id = :itemId and c.ativo and p.ativo""", nativeQuery = true)
    long somarAtivasDoItem(Long itemId);

    // usada ao alterar: soma as outras contribuições, sem a que está sendo alterada
    @Query(value = """
            select coalesce(sum(c.quantidade), 0) from contribuicao c
            join participacao p on p.id = c.participacao_id
            where c.item_id = :itemId and c.id <> :contribuicaoId and c.ativo and p.ativo""", nativeQuery = true)
    long somarOutrasAtivasDoItem(Long itemId, Long contribuicaoId);

    @Query(value = """
            select coalesce(sum(c.quantidade), 0) from contribuicao c
            join participacao p on p.id = c.participacao_id
            join item i on i.id = c.item_id
            where i.categoria_id = :categoriaId and c.ativo and p.ativo""", nativeQuery = true)
    long somarAtivasDaCategoria(Long categoriaId);

    boolean existsByParticipacaoIdAndItemIdAndAtivoTrue(Long participacaoId, Long itemId);

    // não traz o item: ele é carregado depois, já com lock e estado atualizado
    @Query("""
            select c from Contribuicao c
            join fetch c.participacao p join fetch p.participante
            where c.id = :id and p.evento.id = :eventoId and c.ativo = true""")
    Optional<Contribuicao> findAtivaNoEvento(Long id, Long eventoId);

    @Query("""
            select c from Contribuicao c
            join fetch c.participacao p join fetch p.participante pa
            join fetch c.item i join fetch i.categoria
            where p.evento.id = :eventoId and c.ativo = true and p.ativo = true
            order by i.nome, pa.nome""")
    List<Contribuicao> findAtivasDoEvento(Long eventoId);

    @Query("""
            select c from Contribuicao c
            join fetch c.participacao p join fetch p.participante pa
            join fetch c.item i join fetch i.categoria
            where pa.id = :participanteId and c.ativo = true and p.ativo = true
            order by p.evento.dataHora, i.nome""")
    List<Contribuicao> findAtivasDoParticipante(Long participanteId);

    @Modifying
    @Query("update Contribuicao c set c.ativo = false where c.participacao.id = :participacaoId and c.ativo = true")
    int desativarDaParticipacao(Long participacaoId);
}
