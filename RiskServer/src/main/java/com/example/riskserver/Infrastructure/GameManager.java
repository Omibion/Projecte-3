package com.example.riskserver.Infrastructure;

import org.java_websocket.WebSocket;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameManager {
    private static final GameManager instance = new GameManager();
    private final Map<String, GameSession> activeGames = new ConcurrentHashMap<>();
    private final Map<WebSocket, String> sessionToGameMap = new ConcurrentHashMap<>();

    // Singleton pattern
    public static GameManager getInstance() {
        return instance;
    }

    /**
     * Crea una nueva sala de juego
     * @param gameId ID único para la sala
     * @return GameSession creada
     */
    public GameSession createGame(String gameId) {
        if (activeGames.containsKey(gameId)) {
            throw new IllegalArgumentException("Game ID already exists: " + gameId);
        }

        GameSession newGame = new GameSession(gameId);
        activeGames.put(gameId, newGame);
        System.out.println("Nueva sala creada: " + gameId);
        return newGame;
    }

    /**
     * Añade un jugador a una sala existente
     */
    public void addPlayerToGame(String gameId, PlayerSession player) {
        GameSession game = activeGames.get(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }

        System.out.println("Añadiendo jugador (" + player.getPlayerId() + ") a sala " + gameId);
        game.addPlayer(player);
        sessionToGameMap.put(player.getWebSocket(), gameId);
    }

    /**
     * Maneja mensajes entrantes y los redirige a la sala adecuada
     */
    public void handleIncomingMessage(WebSocket session, String payload) {
        String gameId = sessionToGameMap.get(session);
        if (gameId != null) {
            GameSession game = activeGames.get(gameId);
            if (game != null) {
                game.receiveMessage(session, payload);
            }
        }
    }

    /**
     * Maneja la desconexión de un jugador
     */
    public void handleDisconnection(WebSocket session) {
        String gameId = sessionToGameMap.remove(session);
        if (gameId != null) {
            GameSession game = activeGames.get(gameId);
            if (game != null) {
                game.removePlayer(session);

                // Si la sala queda vacía, opcionalmente limpiarla
                if (game.getPlayerCount() == 0) {
                    cleanupEmptyGame(gameId);
                }
            }
        }
    }

    /**
     * Inicia una partida si aún no ha comenzado
     */
    public void startGame(String gameId) {
        GameSession game = activeGames.get(gameId);
        if (game == null) {
            throw new IllegalArgumentException("No se encontró la sala con ID: " + gameId);
        }

        if (game.isStarted()) {
            System.out.println("La partida ya fue iniciada: " + gameId);
            return;
        }

        game.startGame();
        System.out.println("Partida iniciada: " + gameId);
    }

    /**
     * Limpia salas vacías
     */
    private void cleanupEmptyGame(String gameId) {
        GameSession game = activeGames.get(gameId);
        if (game != null && game.getPlayerCount() == 0) {
            game.stopGame();
            activeGames.remove(gameId);
            System.out.println("Sala vacía eliminada: " + gameId);
        }
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
        activeGames.values().forEach(GameSession::stopGame);
        activeGames.clear();
        sessionToGameMap.clear();
    }
}
