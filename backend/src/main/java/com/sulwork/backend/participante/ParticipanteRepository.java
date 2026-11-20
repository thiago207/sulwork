package com.sulwork.backend.participante;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ParticipanteRepository extends JpaRepository<Participante, Long> {

    List<Participante> findByAtivoTrueOrderByNome();

    Optional<Participante> findByIdAndAtivoTrue(Long id);

    Optional<Participante> findByCpf(String cpf);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Participante p where p.id = :id and p.ativo = true")
    Optional<Participante> findAtivoComLock(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Participante p where p.cpf = :cpf")
    Optional<Participante> findByCpfComLock(String cpf);
}
