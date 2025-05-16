package com.example.riskserver.Infrastructure.persistence;

import com.example.riskserver.domain.model.Partida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface PartidaJpaRepository extends JpaRepository<Partida, Integer> {
    List<Partida> findAllByOrderByIdAsc();
    Partida findById(int partidaId);
}
