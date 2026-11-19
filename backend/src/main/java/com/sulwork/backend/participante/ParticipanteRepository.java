package com.sulwork.backend.participante;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipanteRepository extends JpaRepository<Participante, Long> {

    List<Participante> findByAtivoTrueOrderByNome();

    Optional<Participante> findByIdAndAtivoTrue(Long id);

    Optional<Participante> findByCpf(String cpf);
}
