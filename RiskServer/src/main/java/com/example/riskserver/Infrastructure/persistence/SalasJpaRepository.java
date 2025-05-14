package com.example.riskserver.Infrastructure.persistence;

import com.example.riskserver.domain.model.Jugadorp;
import com.example.riskserver.domain.model.Partida;
import com.example.riskserver.domain.model.Sala;
import com.example.riskserver.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalasJpaRepository extends JpaRepository<Partida, Long> {
    public List<Partida> findByEstado(boolean estado);
    @Query("SELECT s FROM Jugador s WHERE s.partida.id = :partidaId")
    List<Jugadorp> findByPartidaId(@Param("partidaId") int partidaId);
    Partida findById(long id);
}
