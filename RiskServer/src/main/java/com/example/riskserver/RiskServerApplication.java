package com.example.riskserver;

import com.example.riskserver.Infrastructure.GameManager;
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
                                             GameManager gameManager) {
        InetSocketAddress port = new InetSocketAddress(29563);
        RiskWebSocket server = new RiskWebSocket(port, loginService,salaService,objectMapper,gameManager);
        server.start();
        return server;
    }
}