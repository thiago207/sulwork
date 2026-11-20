package com.sulwork.backend.contribuicao;

import com.sulwork.backend.cobertura.SomaCategoria;
import com.sulwork.backend.cobertura.SomaItem;
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

    // aliases entre aspas: sem elas o PostgreSQL deixa tudo minúsculo e a projection não acha "itemId"
    @Query(value = """
            select i.id as "itemId", i.nome as "nome", i.obrigatorio as "obrigatorio",
                   i.quantidade_necessaria as "necessario", c.id as "categoriaId", c.unidade as "unidade",
                   coalesce(s.total, 0) as "contribuido"
            from item i
            join categoria c on c.id = i.categoria_id
            left join (
                select ct.item_id, sum(ct.quantidade) as total
                from contribuicao ct
                join participacao p on p.id = ct.participacao_id
                where ct.ativo and p.ativo
                group by ct.item_id
            ) s on s.item_id = i.id
            where c.evento_id = :eventoId
            order by c.nome, i.nome""", nativeQuery = true)
    List<SomaItem> somarPorItemDoEvento(Long eventoId);

    // FILTER soma só as contribuições ativas de participações ativas; categorias sem itens vêm com 0
    @Query(value = """
            select c.id as "categoriaId", c.nome as "nome", c.meta as "meta", c.unidade as "unidade",
                   coalesce(sum(ct.quantidade) filter (where ct.ativo and p.ativo), 0) as "contribuido"
            from categoria c
            left join item i on i.categoria_id = c.id
            left join contribuicao ct on ct.item_id = i.id
            left join participacao p on p.id = ct.participacao_id
            where c.evento_id = :eventoId
            group by c.id, c.nome, c.meta, c.unidade
            order by c.nome""", nativeQuery = true)
    List<SomaCategoria> somarPorCategoriaDoEvento(Long eventoId);

    boolean existsByParticipacaoIdAndItemIdAndAtivoTrue(Long participacaoId, Long itemId);

    // não traz o item: ele é carregado depois, já com lock e estado atualizado
    @Query("""
            select c from Contribuicao c
            join fetch c.participacao p join fetch p.participante
            where c.id = :id and p.evento.id = :eventoId and c.ativo = true and p.ativo = true""")
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

    @Modifying
    @Query("update Contribuicao c set c.ativo = false where c.participacao.participante.id = :participanteId and c.ativo = true")
    int desativarDoParticipante(Long participanteId);
}
