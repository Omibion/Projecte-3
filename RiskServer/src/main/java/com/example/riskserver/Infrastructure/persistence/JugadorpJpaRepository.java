package com.example.riskserver.Infrastructure.persistence;

import com.example.riskserver.domain.model.Jugadorp;
import com.example.riskserver.domain.model.Partida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JugadorpJpaRepository extends JpaRepository<Jugadorp, Integer> {
    List<Jugadorp> findByPartida(Partida partida);
    Jugadorp findById(int id);
}