package com.example.riskserver;

import com.example.riskserver.Infrastructure.GameManager;
import com.example.riskserver.Infrastructure.persistence.JugadorpJpaRepository;
import com.example.riskserver.Infrastructure.persistence.PartidaJpaRepository;
import com.example.riskserver.Infrastructure.persistence.UserJpaRepository;
import com.example.riskserver.Infrastructure.sockets.RiskWebSocket;
import com.example.riskserver.aplication.service.LoginService.LoginService;
import com.example.riskserver.aplication.service.SalaService.SalaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.net.InetSocketAddress;
@SpringBootApplication
public class RiskServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RiskServerApplication.class, args);
    }

    @Bean
    public RiskWebSocket riskWebSocketServer(LoginService loginService,
                                             SalaService salaService,
                                             ObjectMapper objectMapper,
                                             GameManager gameManager,
                                             JugadorpJpaRepository jugadorpJpaRepository,
                                             PartidaJpaRepository partidaJpaRepository,
                                             UserJpaRepository userJpaRepository) {
        InetSocketAddress port = new InetSocketAddress(29563);
        RiskWebSocket server = new RiskWebSocket(port, loginService,salaService,objectMapper,gameManager,jugadorpJpaRepository,partidaJpaRepository, userJpaRepository );
        server.start();
        return server;
    }
}