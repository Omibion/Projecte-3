package com.example.riskserver.Infrastructure.persistence;

import com.example.riskserver.domain.model.Partida;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartidaJpaRepository extends JpaRepository<Partida, Integer> {
    List<Partida> findAllByOrderByIdAsc();
    Partida findById(int partidaId);
}
