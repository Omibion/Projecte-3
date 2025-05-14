package com.example.riskserver.Infrastructure;

import com.example.riskserver.Infrastructure.persistence.*;
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

    // Estado del juego
    private List<JugadorJuego> jugadoresEnPartida;
    private AtomicInteger currentPlayerIndex = new AtomicInteger(0);
    private Map<String, Integer> territorioTropas = new ConcurrentHashMap<>();
    private Map<String, String> territorioJugador = new ConcurrentHashMap<>();
    private Map<String, List<String>> fronterasCache = new ConcurrentHashMap<>();

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
    

    public void addPlayer(PlayerSession player) {
        players.put(player.getPlayerId(), player);
        sessionToPlayer.put(player.getWebSocket(), player.getPlayerId());
        broadcast("Jugador " + player.getPlayerId() + " se ha unido a la sala " + gameId);

        if (players.size() >= 2 && !gameRunning) {
            broadcast("Esperando a que el host inicie la partida...");
        }
    }

    public void startGame(HashMap<String, PlayerSession> jugadores) {
        if (gameRunning) return;

        this.gameRunning = true;
        this.jugadoresEnPartida = buildJugador(gameId);
        inicializarTablero();

        this.gameThread = new Thread(this::runGame);
        this.gameThread.setName("GameThread-" + gameId);
        this.gameThread.start();
    }

    private void runGame() {
        try {
            broadcastEstadoInicial();

            while (gameRunning) {
                JugadorJuego jugadorActual = getJugadorActual();
                iniciarTurno(jugadorActual);

                boolean turnoActivo = true;
                while (turnoActivo && gameRunning) {
                    String message = inputQueue.poll(10, TimeUnit.SECONDS);

                    if (message != null) {
                        turnoActivo = procesarMensaje(jugadorActual, message);
                    }

                   // if (verificarVictoria(jugadorActual)) {
                   //     anunciarVictoria(jugadorActual);
                    //    stopGame();
                   //     break;
                 //   }
                }

                siguienteTurno();
            }
        } catch (InterruptedException e) {
            System.out.println("Partida interrumpida: " + gameId);
        } finally {
            cleanUp();
        }
    }



    private boolean procesarMensaje(JugadorJuego jugador, String message) {
        String[] parts = message.split(":", 3); // Formato: playerId:ACTION:data
        if (!parts[0].equals(jugador.getToken())) {
            sendToPlayer(jugador.getToken(), "No es tu turno");
            return true;
        }

        try {
            switch (parts[1]) {
                case "ATACAR":
                    procesarAtaque(jugador, parts[2]);
                    return true;
                case "FORTIFICAR":
                    procesarFortificacion(jugador, parts[2]);
                    return true;
                case "PASAR_TURNO":
                    return false;
                default:
                    sendToPlayer(jugador.getToken(), "Acción no reconocida");
                    return true;
            }
        } catch (JsonProcessingException e) {
            sendToPlayer(jugador.getToken(), "Error en formato de mensaje");
            return true;
        }
    }

    private void inicializarTablero() {
        // 1. Obtener todos los países y mezclarlos
        List<Pais> todosPaises = paisRepository.findAll();
        Collections.shuffle(todosPaises);

        // 2. Distribuir países equitativamente
        int jugadorIndex = 0;
        for (Pais pais : todosPaises) {
            String tokenJugador = jugadoresEnPartida.get(jugadorIndex).getToken();
            territorioJugador.put(pais.getNom(), tokenJugador);
            territorioTropas.put(pais.getNom(), 1);

            jugadorIndex = (jugadorIndex + 1) % jugadoresEnPartida.size();
        }

        cargarFronterasEnMemoria();

        // 4. Colocación inicial de tropas extras
        int tropasPorJugador = calcularTropasIniciales(jugadoresEnPartida.size());
        for (JugadorJuego jugador : jugadoresEnPartida) {
            jugador.setTropasTurno(tropasPorJugador);
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

    private void broadcastEstadoInicial() {
        Map<String, Object> estado = new HashMap<>();
        estado.put("evento", "INICIO_PARTIDA");
        estado.put("jugadores", jugadoresEnPartida.stream()
                .map(JugadorJuego::getNombre)
                .collect(Collectors.toList()));
        estado.put("mapa", territorioJugador);
        broadcast(toJson(estado));
    }

    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"Error al generar JSON\"}";
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
    private int calcularTropasIniciales(int cantidadJugadores) {

        return switch (cantidadJugadores) {
            case 2 -> 40;
            case 3 -> 35;
            case 4 -> 30;
            case 5 -> 25;
            case 6 -> 20;
            default -> 20;
        };
    }
    private void iniciarTurno(JugadorJuego jugador) {
        // 1. Calcular refuerzos base
        int territorios = (int) territorioJugador.entrySet().stream()
                .filter(e -> e.getValue().equals(jugador.getToken()))
                .count();
        int refuerzos = Math.max(3, territorios / 3);

        // 2. Bonificación por continentes
        refuerzos += calcularBonusContinentes(jugador);

        // 3. Bonificación por cartas (implementar si es necesario)
        // refuerzos += calcularBonusCartas(jugador);

        jugador.setTropasTurno(jugador.getTropasTurno() + refuerzos);

        // 4. Notificar con información detallada
        Map<String, Object> evento = new HashMap<>();
        evento.put("evento", "INICIO_TURNO");
        evento.put("jugador", jugador.getNombre());
        evento.put("refuerzos", refuerzos);
        evento.put("desglose", Map.of(
                "base", Math.max(3, territorios / 3),
                "continentes", calcularBonusContinentes(jugador),
                "total", refuerzos
        ));
        evento.put("tropasDisponibles", jugador.getTropasTurno());

        sendToPlayer(jugador.getToken(), toJson(evento));
    }

    private int calcularBonusContinentes(JugadorJuego jugador) {
        int bonusTotal = 0;
        List<Continent> todosContinentes = continentRepository.findAll();

        for (Continent continente : todosContinentes) {
            boolean controlaContinente = true;
            for (Pais pais : continente.getPaisos()) {
                if (!territorioJugador.get(pais.getNom()).equals(jugador.getToken())) {
                    controlaContinente = false;
                    break;
                }
            }
            if (controlaContinente) {
                bonusTotal += continente.getReforç();
            }
        }

        return bonusTotal;
    }

    private void siguienteTurno() {
        // 1. Rotar jugadores
        currentPlayerIndex.set((currentPlayerIndex.get() + 1) % jugadoresEnPartida.size());

        // 2. Notificar
        JugadorJuego nuevoTurno = getJugadorActual();
        Map<String, Object> evento = new HashMap<>();
        evento.put("evento", "CAMBIO_TURNO");
        evento.put("jugador", nuevoTurno.getNombre());

        broadcast(toJson(evento));
    }


    private JugadorJuego getJugadorActual() {
        return jugadoresEnPartida.get(currentPlayerIndex.get());
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
    // Método mejorado para procesar fortificación
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
}
