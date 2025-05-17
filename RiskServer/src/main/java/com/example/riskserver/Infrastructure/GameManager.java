package com.example.riskserver.Infrastructure;

import com.example.riskserver.Infrastructure.persistence.*;
import com.example.riskserver.aplication.service.GameService.GameService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.WebSocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameManager {
    private static GameManager instance;
    private final Map<String, GameSession> activeGames = new ConcurrentHashMap<>();
    private final Map<WebSocket, String> sessionToGameMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final PartidaJpaRepository partidaRepository;
    private final JugadorpJpaRepository jugadorRepository;
    private final PaisJPARepository paisRepository;
    private final ContinentJPARepository continentRepository;
    private final FronteraJPARepository fronteraRepository;

    // Constructor con inyección de dependencias
    @Autowired
    public GameManager(ObjectMapper objectMapper,
                       PartidaJpaRepository partidaRepository, JugadorpJpaRepository jugadorRepository,
                       PaisJPARepository paisRepository, ContinentJPARepository continentRepository,
                       FronteraJPARepository fronteraRepository) {

        this.objectMapper = objectMapper;
        this.partidaRepository = partidaRepository;
        this.jugadorRepository = jugadorRepository;
        this.paisRepository = paisRepository;
        this.continentRepository = continentRepository;
        this.fronteraRepository = fronteraRepository;
        instance = this;
    }

    // Singleton pattern
    public static GameManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("GameManager no ha sido inicializado. Asegúrate de que Spring lo haya creado.");
        }
        return instance;
    }

    /**
     * Crea una nueva sala de juego
     * @param gameId ID único para la sala
     * @return GameSession creada
     */
    public GameSession createGame(String gameId) {

        GameSession newGame = new GameSession(
                gameId,
                objectMapper,
                partidaRepository,
                jugadorRepository,
                paisRepository,
                continentRepository,
                fronteraRepository

        );

        activeGames.put(gameId, newGame);
        System.out.println("Nueva sala creada: " + gameId);
        return newGame;
    }

    /**
     * Añade un jugador a una sala existente
     */
    public void addPlayerToGame(String gameId, PlayerSession player) {
        GameSession game = getGame(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));

        System.out.println("Añadiendo jugador (" + player.getPlayerId() + ") a sala " + gameId);
        game.addPlayer(player);
        sessionToGameMap.put(player.getWebSocket(), gameId);
    }

    /**
     * Maneja mensajes entrantes y los redirige a la sala adecuada
     */
    /**
     * Maneja mensajes entrantes y los redirige al GameThread específico de la partida
     */
    public void handleIncomingMessage(WebSocket session, String payload) {
        System.out.println("Incoming Message: " + payload);
        String gameId = sessionToGameMap.get(session);
        if (gameId != null) {
            getGame(gameId).ifPresent(game -> {
                // Verifica que la partida esté en ejecución
                if (!game.isGameRunning()) {
                    System.err.println("Partida no iniciada, ignorando mensaje");
                    return;
                }

                // Usar offer() con timeout para evitar bloqueos indefinidos
                boolean success = game.getInputQueue().offer(payload);

                if (!success) {
                    System.err.println("Cola llena, mensaje descartado para gameId: " + gameId);
                    // Opcional: Notificar al jugador que el servidor está ocupado
                    session.send("{\"error\": \"Servidor ocupado, reintenta\"}");
                } else {
                    System.out.println("[DEBUG] Mensaje encolado para gameId: " + gameId + payload);
                }
            });
        }
    }
    public void handlePlayerMessage(String gameId, String token, String message) {
        GameSession session = activeGames.get(gameId);
        if (session != null) {
            session.enqueueMessage(message);
        } else {
            System.err.println("No se encontró la sesión de juego para ID: " + gameId);
        }
    }


    /**
     * Maneja la desconexión de un jugador
     */
    public void handleDisconnection(WebSocket session) {
        String gameId = sessionToGameMap.remove(session);
        if (gameId != null) {
            getGame(gameId).ifPresent(game -> {
                game.removePlayer(session);
                cleanupEmptyGame(gameId);
            });
        }
    }

    /**
     * Inicia una partida si aún no ha comenzado
     */
    public void startGame(String gameId, HashMap<String, PlayerSession> players) {
        GameSession game = getGame(gameId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la sala con ID: " + gameId));

        // Usamos isGameRunning() para verificar el estado
        if (game.isGameRunning()) {
            System.out.println("La partida ya está en curso: " + gameId);
            return;
        }

        game.startGame(players);
        System.out.println("Partida iniciada: " + gameId);
    }
    /**
     * Limpia salas vacías
     */
    private void cleanupEmptyGame(String gameId) {
        getGame(gameId).ifPresent(game -> {
            if (game.getPlayerCount() == 0) {
                game.stopGame();
                activeGames.remove(gameId);
                System.out.println("Sala vacía eliminada: " + gameId);
            }
        });
    }

    /**
     * Obtiene una sala por ID
     */
    public Optional<GameSession> getGame(String gameId) {
        return Optional.ofNullable(activeGames.get(gameId));
    }

    /**
     * Detiene todas las salas (para el cierre del servidor)
     */
    public void shutdown() {
      //  activeGames.values().forEach(GameSession::stopGame);
        activeGames.clear();
        sessionToGameMap.clear();
    }

    /**
     * Obtiene el ID del juego asociado a una sesión WebSocket
     */
    public Optional<String> getGameIdForSession(WebSocket session) {
        return Optional.ofNullable(sessionToGameMap.get(session));
    }

    /**
     * Verifica si un juego existe
     */
    public boolean gameExists(String gameId) {
        return activeGames.containsKey(gameId);
    }
}