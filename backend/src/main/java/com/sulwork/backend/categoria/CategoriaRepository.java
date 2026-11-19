package com.sulwork.backend.categoria;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByEventoIdOrderByNome(Long eventoId);

    Optional<Categoria> findByIdAndEventoId(Long id, Long eventoId);
}
