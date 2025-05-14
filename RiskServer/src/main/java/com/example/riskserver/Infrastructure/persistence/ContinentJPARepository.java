package com.example.riskserver.Infrastructure.persistence;

import com.example.riskserver.domain.model.Continent;
import com.example.riskserver.domain.model.Frontera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContinentJPARepository  extends JpaRepository<Continent, Integer>{
    List<Continent> findAll();
}
