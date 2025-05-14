package com.example.riskserver.Infrastructure;

import com.example.riskserver.Infrastructure.persistence.JugadorpJpaRepository;
import com.example.riskserver.Infrastructure.persistence.PartidaJpaRepository;
import com.example.riskserver.Infrastructure.sockets.RiskWebSocket;
import com.example.riskserver.aplication.service.GameService.GameService;
import com.example.riskserver.domain.model.JugadorJuego;
import com.example.riskserver.domain.model.Jugadorp;
import com.example.riskserver.domain.model.Partida;
import org.java_websocket.WebSocket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class GameSession {
    private final String gameId;
    private final Map<String, PlayerSession> players = new ConcurrentHashMap<>();
    private final Map<WebSocket, String> sessionToPlayer = new ConcurrentHashMap<>();
    private final BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();
    private volatile boolean gameRunning = false;
    private Thread gameThread;
    private JugadorpJpaRepository playerRepository;
    private PartidaJpaRepository partidaRepository;


    public GameSession(String gameId) {
        this.gameId = gameId;
    }

    public void addPlayer(PlayerSession player) {
        players.put(player.getPlayerId(), player);
        sessionToPlayer.put(player.getWebSocket(), player.getPlayerId());
        broadcast("Jugador " + player.getPlayerId() + " se ha unido a la sala " + gameId);

        // Si hay suficientes jugadores se puede iniciar el juego automáticamente si querés
        if (players.size() >= 2 && !gameRunning) {
            broadcast("Esperando a que el host inicie la partida...");
        }
    }

    public boolean hasSession(WebSocket socket) {
        return sessionToPlayer.containsKey(socket);
    }

    public void receiveMessage(WebSocket socket, String payload) {
        String playerId = sessionToPlayer.get(socket);
        if (playerId != null) {
            inputQueue.offer(playerId + ":" + payload);
        }
    }

    public void startGame(HashMap<String, PlayerSession> jugadores) {
        if (gameRunning) return;

        this.gameRunning = true;
        this.gameThread = new Thread(this::runGame);
        this.gameThread.setName("GameThread-" + gameId);
        this.gameThread.start();
        System.out.println("Partida iniciada en sala: " + gameId);
        logicaJuego(gameId, jugadores);
    }

    private void logicaJuego(String gameId, HashMap<String, PlayerSession> jugadores) {

        GameService gameService = new GameService();
        List<JugadorJuego> jugs=buildJugador(gameId);
        JugadorJuego actual=new JugadorJuego();
        jugs= gameService.orden_turno(jugs);
        actual=jugs.get(0);


    }

    public boolean isStarted() {
        return gameRunning;
    }

    private void runGame() {
        try {
            System.out.println("Ejecutando lógica de juego para sala: " + gameId);
            broadcast("¡La partida ha comenzado!");

            while (gameRunning) {
                String message = inputQueue.take();

                if (message.startsWith("ADMIN:")) {
                    handleAdminCommand(message.substring(6));
                } else {
                    String[] parts = message.split(":", 2);
                    if (parts.length == 2) {
                        String playerId = parts[0];
                        String content = parts[1];
                        handlePlayerMessage(playerId, content);
                    }
                }
            }
        } catch (InterruptedException e) {
            System.out.println("Juego interrumpido en sala: " + gameId);
        } finally {
            cleanUp();
        }
    }

    private void handlePlayerMessage(String playerId, String content) {
        broadcast("Jugador " + playerId + ": " + content);

        if (content.startsWith("MOVE:")) {
            processGameMove(playerId, content.substring(5));
        }
    }

    private void processGameMove(String playerId, String move) {
        broadcast("Jugador " + playerId + " realizó un movimiento: " + move);
    }

    private void handleAdminCommand(String command) {
        if ("STOP".equals(command)) {
            stopGame();
        }
    }

    private void broadcast(String message) {
        players.values().forEach(player -> player.send(message));
    }

    public void stopGame() {
        this.gameRunning = false;
        if (gameThread != null) {
            gameThread.interrupt();
        }
        broadcast("La partida ha terminado.");
        cleanUp();
    }

    private void cleanUp() {
        players.values().forEach(PlayerSession::close);
        players.clear();
        sessionToPlayer.clear();
    }

    public void removePlayer(WebSocket socket) {
        String playerId = sessionToPlayer.remove(socket);
        if (playerId != null) {
            players.remove(playerId);
            broadcast("Jugador " + playerId + " ha abandonado la partida");

            if (players.isEmpty()) {
                stopGame();
            }
        }
    }

    public int getPlayerCount() {
        return players.size();
    }

    private List<JugadorJuego> buildJugador(String gameId) {
        Partida p=partidaRepository.findById(Integer.parseInt(gameId));
        List<Jugadorp> jugadores = playerRepository.findByPartida(p);
        List<JugadorJuego> jugadorJuegos = new ArrayList<>();
        for(Jugadorp j:jugadores) {
            JugadorJuego jug = new JugadorJuego();
            jug.setToken(j.getToken());
            jug.setId(j.getId());
            jug.setNombre(j.getNombre());
            jug.setColor(j.getColors());
            jug.setPaisesControlados(new HashMap<>());
            jugadorJuegos.add(jug);
        }
       return jugadorJuegos;
    }
    public void sendToPlayer(String playerId, String message) {
        PlayerSession player = players.get(playerId);
        if (player != null && player.getWebSocket().isOpen()) {
            player.send(message);
        } else {
            System.err.println("No se pudo enviar mensaje al jugador " + playerId + ": conexión cerrada o no existe");
        }
    }
}
