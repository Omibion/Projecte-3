package com.example.riskserver.Infrastructure;

import com.example.riskserver.Infrastructure.persistence.*;
import com.example.riskserver.aplication.dto.*;
import com.example.riskserver.aplication.service.GameService.GameService;
import com.example.riskserver.domain.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.WebSocket;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class GameSession {

    private final String gameId;
    private final Map<String, PlayerSession> players = new ConcurrentHashMap<>();
    private final Map<WebSocket, String> sessionToPlayer = new ConcurrentHashMap<>();
    private final BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();
    private volatile boolean gameRunning = false;
    private Thread gameThread;
    private final ObjectMapper objectMapper;
    private final PartidaJpaRepository partidaRepository;
    private final JugadorpJpaRepository jugadorRepository;
    private final PaisJPARepository paisRepository;
    private final ContinentJPARepository continentRepository;
    private final FronteraJPARepository fronteraRepository;
    private final Map<String, CompletableFuture<String>> seleccionesPendientes = new ConcurrentHashMap<>();
    private Estat currentPhase = Estat.WAIT;
    private final Map<WebSocket, String> connectionToToken = new ConcurrentHashMap<>();
    // Estado del juego
    private List<JugadorJuego> jugadoresEnPartida;
    private final ThreadLocal<Integer> currentPlayerIndex = ThreadLocal.withInitial(() -> 0);

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex.get();
    }

    public void setCurrentPlayerIndex(int index) {
        currentPlayerIndex.set(index);
    }
    private Map<String, Integer> territorioTropas = new ConcurrentHashMap<>();
    private Map<String, String> territorioJugador = new ConcurrentHashMap<>();
    private Map<String, List<String>> fronterasCache = new ConcurrentHashMap<>();

    private final ThreadLocal<PartidaJuego> partidaJuegoThreadLocal = ThreadLocal.withInitial(() -> {
        PartidaJuego partida = new PartidaJuego();
        partida.setJugadores(jugadoresEnPartida);
        return partida;
    });
    // Métodos de acceso al ThreadLocal
    public PartidaJuego getPartidaJuego() {
        return partidaJuegoThreadLocal.get();
    }

    public void setPartidaJuego(PartidaJuego partidaJuego) {
        partidaJuegoThreadLocal.set(partidaJuego);
    }

    public void removePartidaJuego() {
        partidaJuegoThreadLocal.remove();
    }
    public GameSession(String gameId, ObjectMapper objectMapper,  PartidaJpaRepository partidaRepository, JugadorpJpaRepository jugadorRepository, PaisJPARepository paisRepository, ContinentJPARepository continentRepository, FronteraJPARepository fronteraRepository) {
        this.gameId = gameId;
        this.objectMapper = objectMapper;
        this.partidaRepository = partidaRepository;
        this.jugadorRepository = jugadorRepository;
        this.paisRepository = paisRepository;
        this.continentRepository = continentRepository;
        this.fronteraRepository = fronteraRepository;
    }

    public BlockingQueue<String> getInputQueue() {
        return this.inputQueue;
    }

    public boolean isGameRunning() {
        return this.gameRunning;
    }

    private WebSocket getWebSocketByToken(String token) {
        return sessionToPlayer.entrySet().stream()
                .filter(entry -> entry.getValue().equals(token))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public void addPlayer(PlayerSession player) {
        players.put(player.getPlayerId(), player);
        sessionToPlayer.put(player.getWebSocket(), player.getPlayerId());
        connectionToToken.put(player.getWebSocket(), player.getPlayerId()); // Añade esta línea
    }

    public void startGame(HashMap<String, PlayerSession> jugadores) {
        if (gameRunning) return;

        this.gameRunning = true;


        this.gameThread = new Thread(this::runGame);
        this.gameThread.setName("GameThread-" + gameId);
        this.gameThread.start();
    }



    // Método runGame modificado
    private void runGame() {
        try {
            this.jugadoresEnPartida = buildJugador(gameId);
            // Inicialización de la partida para este hilo
            PartidaJuego partidaJuego = getPartidaJuego();
            inicializarTablero(partidaJuego);

            while (gameRunning) {
                String message = inputQueue.poll(60, TimeUnit.SECONDS);
                if (message != null) {
                    System.out.println("[GameThread] Procesando: " + message);
                    procesarMensaje(message);
                }
            }
            //TODO aqui miramos el que ha ganao y se anuncia
        } catch (InterruptedException e) {
            System.out.println("[GameThread] Interrupción recibida");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("[GameThread] Error crítico: " + e.getMessage());
            e.printStackTrace();
        } finally {
            removePartidaJuego(); // Limpieza
            System.out.println("[GameThread] Finalizado para gameId: " + gameId);
        }
    }
    private void procesarMensaje(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            String token = json.get("token").asText();
            String request = json.get("request").asText();

            JugadorJuego jugadorActual = getJugadorActual();

            // Validar que el token coincide con el jugador del turno actual
            if (!jugadorActual.getToken().equals(token)) {
                sendToPlayer(token, "No es tu turno");
                return;
            }

            switch (request) {
                case "seleccionarPaisRQ":
                    handleelegirpais(jugadorActual, json);
                    break;
                case "reforzarPaisRQ":
                    handleReforzarPais(jugadorActual, json);
                    break;
                case "saltarTurnoRQ":
                    siguienteTurno();
                    break;
                case "reforzarTurnoRQ":
                    handleReforzarPais(jugadorActual, json);
                default:
                    sendToPlayer(token, "Acción no válida");
            }
        } catch (JsonProcessingException e) {
            System.err.println("Error al parsear JSON: " + e.getMessage());
        }
    }

    private void handleReforzarPais(JugadorJuego jugadorActual, JsonNode json) {
        ReforzarPaisRQ rq = objectMapper.convertValue(json, ReforzarPaisRQ.class);
        if(rq.getRequest().equals("reforzarTurnoRQ")) {
            jugadorActual.setTotalTropas(jugadorActual.getTotalTropas()+calcularTropasInicioTurno(jugadorActual));
            jugadorActual.setTropasTurno(calcularTropasInicioTurno(jugadorActual));
           // jugadorActual.setTotalTropas(jugadorActual.getTotalTropas()+calcularBonusContinentes(jugadorActual));
          //  jugadorActual.setTropasTurno(jugadorActual.getTropasTurno()+calcularBonusContinentes(jugadorActual));
        }
        int tropasFinal=jugadorActual.getPaisesControlados().get(rq.getNom())+rq.getTropas();
        int tropasQuedan=jugadorActual.getTropasTurno()-rq.getTropas();
        if(jugadorActual.getPaisesControlados().containsKey(rq.getNom())) {
            if(tropasQuedan>=0){
                jugadorActual.getPaisesControlados().put(rq.getNom(),tropasFinal);
                territorioTropas.put(rq.getNom(),tropasFinal);
                PartidaJuego partidaActual = getPartidaJuego();
                jugadorActual.setTropasTurno(tropasQuedan);
                jugadoresEnPartida.set(getCurrentPlayerIndex(), jugadorActual);
                partidaActual.setJugadores(jugadoresEnPartida);
                PartidaRS rs = new  PartidaRS();
                rs.setCode(200);
                rs.setResponse("partidaBC");
                rs.setPartida(partidaActual);
                next();
                int index = getCurrentPlayerIndex();
                rs.getPartida().setTurno(jugadoresEnPartida.get(index).getId());
                int c=0;
                for(JugadorJuego j : partidaActual.jugadores){
                    if(j.getTropasTurno()==0){
                        c++;
                    }
                }
                if(c==partidaActual.jugadores.size()){
                    if(rq.getRequest().equals("reforzarPaisRQ")){
                        currentPhase = Estat.REFORC_TROPES;
                    }
                    else{
                        currentPhase = Estat.COMBAT;
                    }
                    rs.getPartida().setFase(currentPhase);
                    rs.setPartida(partidaActual);
                    broadcast(toJson(rs));
                }else{
                    rs.setPartida(partidaActual);
                    broadcast(toJson(rs));
                }

            }else{
                ErrorRS rs = new ErrorRS();
                rs.setResponse("ErrorRS");
                rs.setCode(401);
                rs.setMesage("No tienes suficientes tropas disponibles para esta acción");
                PlayerSession player = players.get(rq.getToken());
                player.send(toJson(rs));
            }
        }else {
            ErrorRS rs = new ErrorRS();
            rs.setResponse("ErrorRS");
            rs.setCode(402);
            rs.setMesage("El jugador no controla este pais");
        }

    }

    private int calcularTropasInicioTurno(JugadorJuego jugadorActual){
        return jugadorActual.getPaisesControlados().size() / 3;
    }

    private void handleelegirpais(JugadorJuego jugadorActual, JsonNode data) {
        try {
            SeleccionPaisRQ rq = objectMapper.treeToValue(data, SeleccionPaisRQ.class);
            Pais paisSeleccionado = paisRepository.findByNom(rq.getPais());

            // Actualiza el estado del juego en el hilo actual
            PartidaJuego partidaActual = getPartidaJuego();
            jugadorActual.getPaisesControlados().put(paisSeleccionado.getNom(), 1);

            territorioJugador.put(paisSeleccionado.getNom(), jugadorActual.getToken());
            territorioTropas.put(paisSeleccionado.getNom(), 1);
            jugadorActual.setTropasTurno(jugadorActual.getTropasTurno() - 1);

            // Actualiza y envía el estado de la partida
            partidaActual.setTurno(jugadoresEnPartida.get(getCurrentPlayerIndex()).getId());


            PartidaRS partidaRS = new PartidaRS();
            partidaRS.setResponse("partidaBC");
            partidaRS.setCode(200);
            next();
            partidaActual.setTurno(jugadoresEnPartida.get(getCurrentPlayerIndex()).getId());

            int paisesColocaos=0;
            for(JugadorJuego j :  partidaActual.getJugadores()){
                paisesColocaos=+j.getPaisesControlados().size();
            }

            if(paisRepository.count()==paisesColocaos){
                partidaActual.setFase(Estat.REFORC_PAIS);
            }
            partidaRS.setPartida(partidaActual);
            broadcast(toJson(partidaRS));


        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    // Método inicializarTablero modificado
    private void inicializarTablero(PartidaJuego partidaJuego) {
        territorioJugador.clear();
        territorioTropas.clear();

        List<Pais> paisesDisponibles = new ArrayList<>(paisRepository.findAll());
        int tropasPorJugador = calcularTropasIniciales(jugadoresEnPartida.size());

        for (JugadorJuego jugador : jugadoresEnPartida) {
            jugador.setTropasTurno(tropasPorJugador);
            jugador.setTotalTropas(tropasPorJugador);
        }

        partidaJuego.setFase(Estat.COL_LOCAR_INICIAL);
        partidaJuego.setTurno(jugadoresEnPartida.get(0).getId());

        iniciarFaseSeleccionPaises(paisesDisponibles, partidaJuego);
    }

    // Método iniciarFaseSeleccionPaises modificado
    private void iniciarFaseSeleccionPaises(List<Pais> paisesDisponibles, PartidaJuego partidaJuego) {
        currentPhase = Estat.COL_LOCAR_INICIAL;

        PartidaRS p = new PartidaRS();
        p.setResponse("partidaBC");
        partidaJuego.setFase(currentPhase);
        p.setPartida(partidaJuego);
        p.setCode(200);

        broadcast(toJson(p));

        broadcastFaseInicialCompletada(partidaJuego);
    }

    // Método broadcastFaseInicialCompletada modificado
    private void broadcastFaseInicialCompletada(PartidaJuego partidaJuego) {
        Map<String, List<String>> territoriosPorJugador = new HashMap<>();

        for (JugadorJuego jugador : jugadoresEnPartida) {
            List<String> territorios = territorioJugador.entrySet().stream()
                    .filter(e -> e.getValue().equals(jugador.getToken()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            territoriosPorJugador.put(jugador.getNombre(), territorios);
        }

        PartidaRS rs = new PartidaRS();
        rs.setCode(200);
        rs.setResponse("partidaBC");
        partidaJuego.setTurno(jugadoresEnPartida.get(0).getId());
        partidaJuego.setFase(Estat.COL_LOCAR_INICIAL);
        rs.setPartida(partidaJuego);

        broadcast(toJson(rs));
    }

    private Pais esperarSeleccionPais(JugadorJuego jugador) {
        CompletableFuture<String> seleccionFuture = new CompletableFuture<>();
        seleccionesPendientes.put(jugador.getToken(), seleccionFuture);

        try {
            // Espera activamente procesando mensajes hasta que llegue la selección
            while (true) {
                String message = inputQueue.poll(60, TimeUnit.SECONDS);
                if (message == null) {
                    break; // Timeout
                }

                try {
                    JsonNode json = objectMapper.readTree(message);
                    if (json.has("token") && json.get("token").asText().equals(jugador.getToken())) {
                        if (json.has("request") && "seleccionarPaisRQ".equals(json.get("request").asText())) {
                            return paisRepository.findByNom(json.get("data").get("pais").asText());
                        }
                    }
                } catch (Exception e) {
                    // Mensaje no válido, continuar esperando
                    continue;
                }
            }

            // Timeout - seleccionar aleatorio
            return seleccionarPaisAleatorio(jugador);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return seleccionarPaisAleatorio(jugador);
        } finally {
            seleccionesPendientes.remove(jugador.getToken());
        }
    }

    private Pais seleccionarPaisAleatorio(JugadorJuego jugador) {
        List<Pais> disponibles = paisRepository.findAll().stream()
                .filter(p -> !territorioJugador.containsKey(p.getNom()))
                .collect(Collectors.toList());

        if (!disponibles.isEmpty()) {
            return disponibles.get(new Random().nextInt(disponibles.size()));
        }
        throw new IllegalStateException("No hay países disponibles para seleccionar");
    }

    private void handleTurno(JugadorJuego jugadorActual, JsonNode data){

    }

    private void broadcastPaisSeleccionado(Pais pais, JugadorJuego jugador) {
        Map<String, Object> mensaje = new HashMap<>();
        mensaje.put("evento", "PAIS_SELECCIONADO");
        mensaje.put("pais", pais.getNom());
        mensaje.put("jugador", jugador.getNombre());
        mensaje.put("tropas", 1);

        broadcast(toJson(mensaje));
    }

    private void handleSeleccionPais(WebSocket conn, JsonNode jsonNode) {
        String token = connectionToToken.get(conn);
        if (token == null) return;

        if (currentPhase != Estat.COL_LOCAR_INICIAL) {
            sendError(conn, "No es momento de seleccionar países", token);
            return;
        }

        String nombrePais = jsonNode.get("pais").asText();
        CompletableFuture<String> future = seleccionesPendientes.get(token);

        if (future != null) {
            future.complete(nombrePais);
            seleccionesPendientes.remove(token);
        }
    }

    private void cargarFronterasEnMemoria() {
        // Precargar todas las fronteras para validaciones rápidas
        List<Frontera> todasFronteras = fronteraRepository.findAll();
        Map<String, List<String>> tempCache = new HashMap<>(); // Temporal para carga

        for (Frontera frontera : todasFronteras) {
            Pais pais1 = frontera.getPais1();
            Pais pais2 = frontera.getPais2();

            tempCache.computeIfAbsent(pais1.getNom(), k -> new ArrayList<>()).add(pais2.getNom());
            tempCache.computeIfAbsent(pais2.getNom(), k -> new ArrayList<>()).add(pais1.getNom());
        }

        // Asignación atómica al cache final
        this.fronterasCache = new ConcurrentHashMap<>(tempCache);
    }
    private void procesarAtaque(JugadorJuego jugador, String data) throws JsonProcessingException {
        JsonNode json = objectMapper.readTree(data);
        String desde = json.get("desde").asText();
        String hacia = json.get("hacia").asText();
        int tropas = json.get("tropas").asInt();

        if (!validarAtaque(jugador, desde, hacia, tropas)) {
            sendToPlayer(jugador.getToken(), "Ataque inválido");
            return;
        }

        // Resolver batalla
        boolean exito = resolverBatalla(
                territorioTropas.get(desde),
                territorioTropas.get(hacia)
        );

        if (exito) {
            territorioJugador.put(hacia, jugador.getToken());
            territorioTropas.put(hacia, tropas);
            territorioTropas.merge(desde, -tropas, Integer::sum);

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("evento", "ATAQUE_EXITOSO");
            respuesta.put("territorio", hacia);
            respuesta.put("nuevoDueño", jugador.getNombre());
            broadcast(toJson(respuesta));
        } else {
            sendToPlayer(jugador.getToken(), "Ataque fallido");
        }
    }

    private boolean validarAtaque(JugadorJuego jugador, String desde, String hacia, int tropas) {
        return territorioJugador.get(desde).equals(jugador.getToken()) &&
                !territorioJugador.get(hacia).equals(jugador.getToken()) &&
                territorioTropas.get(desde) > tropas &&
                tropas > 0;
    }



    private void sendError(WebSocket conn, String errorMessage, String token) {
        try {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("evento", "ERROR");
            errorResponse.put("mensaje", errorMessage);
            errorResponse.put("faseActual", currentPhase.toString());

            String jsonError = objectMapper.writeValueAsString(errorResponse);
            conn.send(jsonError);
        } catch (JsonProcessingException e) {
            System.err.println("Error al generar mensaje de error para " + token + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error al enviar mensaje a " + token + ": " + e.getMessage());
        }
    }

    private void broadcastEstadoInicial() {
        Map<String, Object> estado = new HashMap<>();
        estado.put("evento", "INICIO_PARTIDA");
        estado.put("jugadores", jugadoresEnPartida.stream()
                .map(JugadorJuego::getNombre)
                .collect(Collectors.toList()));
        estado.put("mapa", territorioJugador);
        broadcast(toJson(estado));
    }

    private void broadcastFaseInicialCompletada() {
        Map<String, Object> mensaje = new HashMap<>();

        PartidaJuego jj = new PartidaJuego();
        // Agrupar territorios por jugador para facilitar el procesamiento en el cliente
        Map<String, List<String>> territoriosPorJugador = new HashMap<>();
        jj.setJugadores(jugadoresEnPartida);
        for (JugadorJuego jugador : jugadoresEnPartida) {
            List<String> territorios = territorioJugador.entrySet().stream()
                    .filter(e -> e.getValue().equals(jugador.getToken()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            territoriosPorJugador.put(jugador.getNombre(), territorios);

        }
        PartidaRS rs = new PartidaRS();
        rs.setCode(200);
        rs.setResponse("partidaBC");
        jj.setTurno(jugadoresEnPartida.get(0).getId());
        jj.setJugadores(jugadoresEnPartida);
        jj.setFase(Estat.COL_LOCAR_INICIAL);
        rs.setPartida(jj);
        System.out.println(toJson(rs));
        broadcast(toJson(rs));
    }



    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"Error al generar JSON\"}";
        }
    }


    private void broadcast(String message) {
        System.out.println("Broadcast: " + message);
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
    private int calcularTropasIniciales(int cantidadJugadores) {

        return switch (cantidadJugadores) {
            case 1 -> 80;
            case 2 -> 40;
            case 3 -> 35;
            case 4 -> 30;
            case 5 -> 25;
            case 6 -> 20;
            default -> 20;
        };
    }
    private void iniciarTurno(JugadorJuego jugador) {
        // 1. Calcular refuerzos (como ya lo tienes)
        int territorios = (int) territorioJugador.entrySet().stream()
                .filter(e -> e.getValue().equals(jugador.getToken()))
                .count();
        int refuerzos = Math.max(3, territorios / 3);
        refuerzos += calcularBonusContinentes(jugador);
        jugador.setTropasTurno(jugador.getTropasTurno() + refuerzos);

        Map<String, Object> turnoInfo = new HashMap<>();
        turnoInfo.put("evento", "TURNO_INICIADO");
        turnoInfo.put("jugador", jugador.getNombre());
        turnoInfo.put("refuerzos", refuerzos);
        turnoInfo.put("tropasDisponibles", jugador.getTropasTurno());
        turnoInfo.put("faseActual", currentPhase.toString());


        List<Map<String, Object>> territoriosInfo = territorioJugador.entrySet().stream()
                .filter(e -> e.getValue().equals(jugador.getToken()))
                .map(e -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("nombre", e.getKey());
                    info.put("tropas", territorioTropas.get(e.getKey()));
                    info.put("fronteras", fronterasCache.getOrDefault(e.getKey(), Collections.emptyList()));
                    return info;
                })
                .collect(Collectors.toList());
        turnoInfo.put("territorios", territoriosInfo);

        // 4. Enviar notificación solo al jugador actual
        sendToPlayer(jugador.getToken(), toJson(turnoInfo));

        // 5. Notificar a los otros jugadores (opcional)
        Map<String, Object> notificacionOtros = new HashMap<>();
        notificacionOtros.put("evento", "TURNO_DE_OTRO");
        notificacionOtros.put("jugadorActual", jugador.getNombre());
        notificacionOtros.put("faseActual", currentPhase.toString());

        // Enviar a todos excepto al jugador actual
        players.values().stream()
                .filter(p -> !p.getPlayerId().equals(jugador.getToken()))
                .forEach(p -> p.send(toJson(notificacionOtros)));
    }

    private int calcularBonusContinentes(JugadorJuego jugador) {
        int bonusTotal = 0;
        List<Continent> todosContinentes = continentRepository.findAll();

        for (Continent continente : todosContinentes) {
            boolean controlaAhora = true;
            for (Pais pais : continente.getPaisos()) {
                if (!jugador.getPaisesControlados().containsKey(pais.getNom())) {
                    controlaAhora = false;
                    break;
                }
            }

            // Si controla el continente ahora...
            if (controlaAhora) {
                // ¿Ya lo controlaba antes? (verifica si ya recibió el bonus)
                boolean yaLoControlaba = jugador.getContinentesControlados().contains(continente.getId());

                if (!yaLoControlaba) {
                    // Es nuevo: aplicar bonus y marcarlo como controlado
                    bonusTotal += continente.getReforç();
                    jugador.getContinentesControlados().add(continente.getId());
                }
            } else {
                // Ya no lo controla (quizá perdió un territorio): remover de controlados
                jugador.getContinentesControlados().remove(continente.getId());
            }
        }
        return bonusTotal;
    }

    private void siguienteTurno() {
        // Rotar jugadores
        next();
        PartidaJuego partidaActual = getPartidaJuego();

        partidaActual.setTurno(jugadoresEnPartida.get(getCurrentPlayerIndex()).getId());
        PartidaRS rs = new  PartidaRS();
        rs.setCode(200);
        rs.setResponse("partidaRS");
        rs.setPartida(partidaActual);
        broadcast(toJson(rs));

    }

    private JugadorJuego getJugadorActual() {
        return jugadoresEnPartida.get(getCurrentPlayerIndex());
    }

    public int getPlayerCount() {
        return players.size();
    }

    private List<JugadorJuego> buildJugador(String gameId) {
        Partida p=partidaRepository.findById(Integer.parseInt(gameId));
        List<Jugadorp> jugadores = jugadorRepository.findByPartida(p);
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
    private void procesarFortificacion(JugadorJuego jugador, String data) throws JsonProcessingException {
        JsonNode json = objectMapper.readTree(data);
        String desde = json.get("desde").asText();
        String hacia = json.get("hacia").asText();
        int tropas = json.get("tropas").asInt();

        if (!validarFortificacion(jugador, desde, hacia, tropas)) {
            sendToPlayer(jugador.getToken(), "Fortificación inválida");
            return;
        }

        // Validar ruta terrestre (cadena de territorios aliados)
        if (!validarRutaTerrestre(jugador.getToken(), desde, hacia, new HashSet<>())) {
            sendToPlayer(jugador.getToken(), "No hay ruta terrestre válida");
            return;
        }

        // Mover tropas
        territorioTropas.merge(desde, -tropas, Integer::sum);
        territorioTropas.merge(hacia, tropas, Integer::sum);

        // Notificar
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("evento", "FORTIFICACION");
        respuesta.put("desde", desde);
        respuesta.put("hacia", hacia);
        respuesta.put("tropas", tropas);

        broadcast(toJson(respuesta));
    }
    private boolean validarFortificacion(JugadorJuego jugador, String desde, String hacia, int tropas) {
        return territorioJugador.get(desde).equals(jugador.getToken()) &&
                territorioJugador.get(hacia).equals(jugador.getToken()) &&
                territorioTropas.get(desde) > tropas &&
                tropas > 0 &&
                sonTerritoriosAdyacentes(desde, hacia); // Implementar esta función
    }
    private boolean validarRutaTerrestre(String tokenJugador, String actual, String destino, Set<String> visitados) {
        if (actual.equals(destino)) return true;
        visitados.add(actual);

        for (String vecino : fronterasCache.getOrDefault(actual, Collections.emptyList())) {
            if (!visitados.contains(vecino) &&
                    territorioJugador.get(vecino).equals(tokenJugador)) {
                if (validarRutaTerrestre(tokenJugador, vecino, destino, visitados)) {
                    return true;
                }
            }
        }

        return false;
    }
    private boolean resolverBatalla(int atacantes, int defensores) {
        // Implementación básica con dados (puedes mejorarla)
        Random rand = new Random();

        int dadoAtacante = rand.nextInt(6) + 1;
        int dadoDefensor = rand.nextInt(6) + 1;

        if (dadoAtacante > dadoDefensor) {
            return true; // Atacante gana
        } else if (dadoAtacante == dadoDefensor) {
            return rand.nextBoolean(); // Empate, decisión aleatoria
        } else {
            return false; // Defensor gana
        }
    }

    private boolean sonTerritoriosAdyacentes(String territorio1, String territorio2) {
        return fronterasCache.getOrDefault(territorio1, Collections.emptyList())
                .contains(territorio2);
    }
    private boolean validarAtaqueComplejo(JugadorJuego jugador, String desde, String hacia, int tropas) {
        // 1. Validar propiedad
        if (!territorioJugador.get(desde).equals(jugador.getToken())) return false;

        // 2. Validar que no sea propio territorio
        if (territorioJugador.get(hacia).equals(jugador.getToken())) return false;

        // 3. Validar adyacencia
        if (!sonTerritoriosAdyacentes(desde, hacia)) return false;

        // 4. Validar cantidad de tropas
        if (territorioTropas.get(desde) <= tropas) return false;

        // 5. Validar mínimo de tropas (opcional)
        return tropas >= 1;
    }
    public void handleWebSocketMessage(WebSocket conn, String message) {
        String token = connectionToToken.get(conn);
        if (token != null) {
            inputQueue.add(message);
        }
    }
    public void enqueueMessage(String message) {
        inputQueue.add(message);
    }
    public void next(){
        setCurrentPlayerIndex((getCurrentPlayerIndex() + 1) % jugadoresEnPartida.size());
    }
    public void perderPais(JugadorJuego jugador, String nombrePais) {
        jugador.getPaisesControlados().remove(nombrePais);

        // Verificar si perdió el control de algún continente
        Pais paisPerdido = paisRepository.findByNom(nombrePais);
        Continent continenteAfectado = paisPerdido.getContinent();

        boolean aunControlaContinente = continenteAfectado.getPaisos().stream()
                .allMatch(p -> jugador.getPaisesControlados().containsKey(p.getNom()));

        if (!aunControlaContinente) {
            jugador.getContinentesControlados().remove(continenteAfectado.getId());
        }
    }
}