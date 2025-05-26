package com.example.riskserver.Infrastructure.sockets;

import com.example.riskserver.Infrastructure.GameManager;
import com.example.riskserver.Infrastructure.PlayerSession;
import com.example.riskserver.Infrastructure.persistence.JugadorpJpaRepository;
import com.example.riskserver.Infrastructure.persistence.PartidaJpaRepository;
import com.example.riskserver.aplication.dto.*;
import com.example.riskserver.aplication.service.LoginService.LoginService;
import com.example.riskserver.aplication.service.SalaService.SalaService;
import com.example.riskserver.domain.model.Jugadorp;
import com.example.riskserver.domain.model.Partida;
import com.example.riskserver.domain.model.Sala;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RiskWebSocket extends WebSocketServer {
    // Map of token → player session
    private final Map<String, PlayerSession> tokenToSession = new ConcurrentHashMap<>();

    // Map of connection → token (to know which token each WebSocket uses)
    private final Map<WebSocket, String> connectionToToken = new ConcurrentHashMap<>();

    // Map of token → game room id
    private final Map<String, Integer> tokenToGame = new ConcurrentHashMap<>();

    // Map of userId → token
    private final Map<Integer, String> userToToken = new ConcurrentHashMap<>();

    private final LoginService loginService;
    private final SalaService salaService;
    private final ObjectMapper objectMapper;
    private final GameManager gameManager;
    private final JugadorpJpaRepository  jugadorpJpaRepository;
    private final PartidaJpaRepository partidaJpaRepository;

    public RiskWebSocket(InetSocketAddress address,
                         LoginService loginService,
                         SalaService salaService,
                         ObjectMapper objectMapper,
                         GameManager gameManager,
                         JugadorpJpaRepository jugadorpJpaRepository,
                         PartidaJpaRepository partidaJpaRepository) {
        super(address);
        this.loginService = loginService;
        this.salaService = salaService;
        this.objectMapper = objectMapper;
        this.gameManager = gameManager;
        this.jugadorpJpaRepository = jugadorpJpaRepository;
        this.partidaJpaRepository = partidaJpaRepository;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String clientId = conn.getRemoteSocketAddress().toString();
        System.out.println("New user connected: " + clientId);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String clientId = conn.getRemoteSocketAddress().toString();
        String token = connectionToToken.remove(conn);

        if (token != null) {
            PlayerSession session = tokenToSession.remove(token);
            Integer gameId = tokenToGame.remove(token);

            // Find and remove user id from userToToken
            for (Map.Entry<Integer, String> entry : userToToken.entrySet()) {
                if (entry.getValue().equals(token)) {
                    Integer userId = entry.getKey();
                    userToToken.remove(userId);

                    // If user was in a game room, notify others about departure
                    if (gameId != null) {
                        try {
                            salaService.leaveUserFromSala(gameId, userId);

                            // Get updated room and broadcast to remaining users
                            Sala updatedSala = salaService.getSala(gameId);
                           // okupaJPARepository.deleteAll();
                            if (updatedSala != null && !updatedSala.getJugadores().isEmpty()) {
                                UpdateUsersBC bc = new UpdateUsersBC();
                                bc.setResponse("updateUserBC");
                                bc.setCode(200);
                                bc.setSala(updatedSala);

                                broadcastToSala(gameId, bc);
                            }
                        } catch (Exception e) {
                            System.err.println("Error handling disconnect from room: " + e.getMessage());
                        }
                    }
                    break;
                }
            }
        }

        System.out.println("User disconnected: " + clientId);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        String clientId = conn.getRemoteSocketAddress().toString();

        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            String requestType = jsonNode.get("request").asText();

            switch (requestType) {
                case "login":
                    handleLogin(conn, jsonNode);
                    break;
                case "getSalasRQ":
                    handleSalas(conn, jsonNode);
                    break;
                case "createSalaRQ":
                    handleCreateSala(conn, jsonNode);
                    break;
                case "joinSalaRQ":
                    handleJoinSala(conn, jsonNode);
                    break;
                case "leaveSalaRQ":
                    handleLeaveSala(conn, jsonNode);
                    break;
                case "updateSalaRQ":
                    handleUpdateSala(conn, jsonNode);
                    break;
                case "reauth":
                    handleReconnect(conn, jsonNode);
                    break;
                case "seleccionarPaisRQ":
                case "saltarTurnoRQ":
                case "reforzarPaisRQ":
                case "reforzarTurnoRQ":
                case "atacarRQ":
                case "meAtacanRQ":
                case "moverTropasRQ":
                case "conquistaRQ":
                case "tunearJueguitoRQ":
                    handleGameMessage(conn, jsonNode);
                    break;
                default:
                    String token = connectionToToken.get(conn);
                    sendError(conn, "Request type not recognized: " + requestType, token);
            }
        } catch (Exception e) {
            String token = connectionToToken.get(conn);
            sendError(conn, "Error processing message: " + e.getMessage(), token);
            e.printStackTrace();
        }
    }

    private void handleGameMessage(WebSocket conn, JsonNode message) {
        String token = connectionToToken.get(conn);
        if (token == null) {
            sendError(conn, "No autenticado", null);
            return;
        }

        System.out.println("Message received from " + token + ": " + message);

        Integer gameId = tokenToGame.get(token);
        if (gameId == null) {
            sendError(conn, "No estás en ninguna partida", token);
            return;
        }

        // Delegar el mensaje al GameManager/GameSession correspondiente
        gameManager.handleIncomingMessage(conn,message+"");//habia un .assText pero no iba
    }

    private void handleUpdateSala(WebSocket conn, JsonNode jsonNode) throws JsonProcessingException {
        UpdateSalaRQ rq = objectMapper.treeToValue(jsonNode, UpdateSalaRQ.class);
        UpdateUsersBC bc = new UpdateUsersBC();
        bc.setResponse("updateUserBC");
        bc.setCode(200);
        bc.setSala(rq.getSala());
        for(Jugadorp j : rq.getSala().getJugadores()) {
            int id = rq.getSala().getId();
            j.setPartida(partidaJpaRepository.findById(id));
            jugadorpJpaRepository.save(j);
        }
        broadcastToSala(rq.getSala().getId(), bc);

        // Verifica si todos los jugadores están listos
        if (rq.getSala().getJugadores().size() > 0) {
            int c = 0;
            for (Jugadorp j : rq.getSala().getJugadores()) {
                if (j.isEstado()) {
                    c++;
                }
            }

            // Si todos los jugadores están listos, iniciar la partida
            if (c == rq.getSala().getJugadores().size()) {
                System.out.println("Todos los jugadores están listos. Iniciando partida...");
                gameManager.createGame(rq.getSala().getId()+"");

                HashMap<String, PlayerSession> jugadoresASala = new HashMap<>();
                for(Jugadorp j : rq.getSala().getJugadores()) {
                    PlayerSession p = tokenToSession.get(j.getToken());
                    jugadoresASala.put(j.getToken(), p);
                    gameManager.addPlayerToGame(rq.getSala().getId()+"",p);
                }



                // Notificar a los clientes
                gameManager.startGame(rq.getSala().getId()+"", jugadoresASala);
            }
        }
    }


    private void handleLeaveSala(WebSocket conn, JsonNode jsonNode) throws JsonProcessingException {
        LeaveSalaRQ rq = objectMapper.treeToValue(jsonNode, LeaveSalaRQ.class);
        LeaveSalaRS rs = new LeaveSalaRS();
        rs.setCode(200);
        rs.setResponse("leaveSalaRS");
        sendResponse(conn, rs, rq.getToken());
        tokenToGame.remove(rq.getToken());
        Sala updatedSala = salaService.getSala(rq.getIdSala());
        salaService.leaveUserFromSala(rq.getIdSala(), rq.getIdUsuari());
        if (updatedSala != null && !updatedSala.getJugadores().isEmpty()) {
            UpdateUsersBC bc = new UpdateUsersBC();
            bc.setResponse("updateUserBC");
            bc.setCode(200);
            bc.setSala(updatedSala);
            broadcastToSala(rq.getIdSala(), bc);
        }
    }

    private void handleJoinSala(WebSocket conn, JsonNode jsonNode) throws JsonProcessingException {
        JoinSalaRQ rq = objectMapper.treeToValue(jsonNode, JoinSalaRQ.class);
        UpdateUsersBC bc = new UpdateUsersBC();
        Sala sala = null;
        if (rq.getSala() < 0 || rq.getUser() < 0) {
            sendError(conn, "Invalid room or user number.", rq.getToken());
            return;
        }

        sala = salaService.addUserToSala(rq.getSala(), rq.getUser(), rq.getToken());

        JoinSalaRS rs = new JoinSalaRS();
        rs.setResponse("joinSalaRS");
        rs.setCode(200);
        rs.setSala(sala);
        tokenToGame.put(rq.getToken(), sala.getId());
        sendResponse(conn, rs, rq.getToken());
        bc.setCode(200);
        bc.setResponse("updateUserBC");
        bc.setSala(sala);
        broadcastToSala(sala.getId(), bc);
    }

    private void handleCreateSala(WebSocket conn, JsonNode jsonNode) {
        try {
            CreateSalaRQ rq = objectMapper.treeToValue(jsonNode, CreateSalaRQ.class);

            Sala sala = new Sala();
            sala.setNombre(rq.getName());
            sala.setMaxPlayers(3);
            Sala nuevaSala = salaService.createSala(sala, rq.getUser(),rq.getToken());
            gameManager.createGame("sala-" + nuevaSala.getId());

            // Update token to game mapping
            tokenToGame.put(rq.getToken(), nuevaSala.getId());

            CreateSalaRS rs = new CreateSalaRS();
            rs.setResponse("createSalaRS");
            rs.setSala(nuevaSala);
            rs.setCode(200);
            sendResponse(conn, rs, rq.getToken());

        } catch (Exception e) {
            String token = connectionToToken.get(conn);
            sendError(conn, "Error creating room: " + e.getMessage(), token);
            e.printStackTrace();
        }
    }

    private void handleSalas(WebSocket conn, JsonNode jsonNode) {
        try {
            GetSalasRQ rq = objectMapper.treeToValue(jsonNode, GetSalasRQ.class);


            List<Sala> salas = salaService.getAllSalas();
            if (salas == null) {
                salas = new ArrayList<>();
            }

            // Clear sensitive player data before sending
            for (Sala s : salas) {
                if (s.getJugadores() == null) {
                    s.setJugadores(new ArrayList<>());
                }
            }

            GetSalasRS rs = new GetSalasRS();
            rs.setResponse("getSalasRS");
            rs.setSalas(salas);
            rs.setCode(200);

            sendResponse(conn, rs, rq.getToken());

        } catch (JsonProcessingException e) {
            String token = connectionToToken.get(conn);
            sendError(conn, "Error getting rooms: " + e.getMessage(), token);
        }
    }

    private void handleLogin(WebSocket conn, JsonNode jsonNode) {
        try {
            LoginRQ loginRQ = objectMapper.treeToValue(jsonNode, LoginRQ.class);

            if (loginRQ.getUsername() == null || loginRQ.getPassword() == null) {
                sendError(conn, "Username and password are required", null);
                return;
            }

            LoginRS loginRS = loginService.login(loginRQ.getUsername(), loginRQ.getPassword());

            if (loginRS.getCode() != 200 || loginRS.getUser() == null) {
                sendError(conn, "Login failed: Invalid credentials", null);
                return;
            }

            // Generate token
            String token = java.util.UUID.randomUUID().toString();

            loginRS.setToken(token);

            // Store session information
            PlayerSession player = new PlayerSession(token, conn);
            tokenToSession.put(token, player);
            connectionToToken.put(conn, token);
            userToToken.put(loginRS.getUser().getId(), token);

            sendResponse(conn, loginRS, token);
        } catch (Exception e) {
            sendError(conn, "Error during login: " + e.getMessage(), null);
            e.printStackTrace();
        }
    }

    private void sendResponse(WebSocket conn, Object response, String token) {
        try {
            String jsonResponse = objectMapper.writeValueAsString(response);
            conn.send(jsonResponse);
            System.out.println("Response sent: " + jsonResponse);
        } catch (Exception e) {
            System.err.println("Error sending response: " + e.getMessage());
        }
    }

    private void sendError(WebSocket conn, String errorMessage, String token) {
        Map<String, Object> errorResponse = Map.of(
                "response", "error",
                "code", 400,
                "message", errorMessage,
                "success", false,
                "token", token != null ? token : ""
        );

        try {
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            conn.send(jsonResponse);
        } catch (Exception e) {
            System.err.println("Error sending error response: " + e.getMessage());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("WebSocket error: ");
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket server started on port: " + this.getPort());
    }

    public void broadcastToSala(int salaId, Object messageObject) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(messageObject);
            List<String> tokensInSala = new ArrayList<>();

            // Find all tokens associated with this room
            for (Map.Entry<String, Integer> entry : tokenToGame.entrySet()) {
                if (entry.getValue() == salaId) {
                    tokensInSala.add(entry.getKey());
                }
            }

            // Send message to all users in the room
            for (String token : tokensInSala) {
                PlayerSession session = tokenToSession.get(token);
                if (session != null && session.getWebSocket().isOpen()) {
                    System.out.println("Enviado a: "+session.toString()+jsonMessage);
                    session.getWebSocket().send(jsonMessage);
                }
            }
        } catch (JsonProcessingException e) {
            System.err.println("Error converting message to JSON: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error in broadcast: " + e.getMessage());
        }
    }

    private void handleReconnect(WebSocket conn, JsonNode jsonNode) throws JsonProcessingException {
        String token = jsonNode.get("token").asText();

        PlayerSession session = tokenToSession.get(token);
        if (session == null) {
            sendError(conn, "Invalid token or session expired", token);
            return;
        }
        session.setWebSocket(conn);
        connectionToToken.put(conn, token);
        Integer userId = null;
        for (Map.Entry<Integer, String> entry : userToToken.entrySet()) {
            if (entry.getValue().equals(token)) {
                userId = entry.getKey();
                break;
            }
        }

        if (userId == null) {
            sendError(conn, "User not found for token", token);
            return;
        }

        Integer salaId = tokenToGame.get(token);
        if (salaId != null) {
            Sala sala = salaService.getSala(salaId);
            if (sala != null) {
                JoinSalaRS rs = new JoinSalaRS();
                rs.setCode(200);
                rs.setResponse("joinSalaRS");
                rs.setSala(sala);
                sendResponse(conn, rs, token);
            }
        }
    }

}