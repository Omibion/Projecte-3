package com.example.riskserver.Infrastructure.Config;

import com.example.riskserver.Infrastructure.GameManager;
import com.example.riskserver.Infrastructure.sockets.RiskWebSocket;
import com.example.riskserver.aplication.service.LoginService.LoginService;
import com.example.riskserver.aplication.service.SalaService.SalaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;

@Configuration
public class WebSocketConfig {

    @Value("${websocket.host:localhost}") // Valor por defecto 'localhost'
    private String host;

    @Value("${websocket.port:29563}")
    private int port;

    @Bean
    public InetSocketAddress webSocketAddress() {
        return new InetSocketAddress(host, port);
    }

    @Bean
    public RiskWebSocket riskWebSocket(InetSocketAddress address,
                                       LoginService loginService,
                                       SalaService salaService,
                                       ObjectMapper objectMapper,
                                       GameManager gameManager) {
        return new RiskWebSocket(address, loginService,salaService, objectMapper, gameManager);
    }
}