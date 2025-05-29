package com.example.riskserver.aplication.service.FronterasService;

import com.example.riskserver.Infrastructure.persistence.FronteraJPARepository;
import com.example.riskserver.domain.model.Frontera;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FronteraService {

    private Map<String, List<String>> fronterasCache;

    @Autowired
    private FronteraJPARepository fronteraRepository;

    @PostConstruct
    public void init() {
        cargarFronterasEnMemoria();
    }

    private void cargarFronterasEnMemoria() {
        List<Frontera> todasFronteras = fronteraRepository.findAll();
        Map<String, List<String>> tempCache = new HashMap<>();

        for (Frontera frontera : todasFronteras) {
            String p1 = frontera.getPais1().getNom();
            String p2 = frontera.getPais2().getNom();

            tempCache.computeIfAbsent(p1, k -> new ArrayList<>()).add(p2);
            tempCache.computeIfAbsent(p2, k -> new ArrayList<>()).add(p1);
        }


        Map<String, List<String>> immutableCache = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : tempCache.entrySet()) {
            immutableCache.put(entry.getKey(), List.copyOf(entry.getValue()));
        }

        this.fronterasCache = Collections.unmodifiableMap(immutableCache);
    }

}
