package com.example.riskserver.Infrastructure.persistence;

import com.example.riskserver.domain.model.Okupa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface OkupaJPARepository extends JpaRepository<Okupa, Integer> {
    public Okupa findById(int id);

    @Transactional
    @Modifying
    @Query("DELETE FROM Okupa o WHERE o.jugador.id = :jugadorId")
    void deleteByJugadorId(@Param("jugadorId") Long jugadorId);
}
