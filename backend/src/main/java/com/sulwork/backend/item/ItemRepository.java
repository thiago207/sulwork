package com.sulwork.backend.item;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ItemRepository extends JpaRepository<Item, Long> {

    // join fetch traz a categoria na mesma consulta (evita N+1 ao ler a unidade)
    @Query("""
            select i from Item i join fetch i.categoria c
            where c.evento.id = :eventoId
            order by c.nome, i.nome""")
    List<Item> findByEvento(Long eventoId);

    @Query("select i from Item i join fetch i.categoria c where i.id = :id and c.evento.id = :eventoId")
    Optional<Item> findByIdAndEvento(Long id, Long eventoId);

    boolean existsByCategoriaId(Long categoriaId);
}
