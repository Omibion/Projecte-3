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
            cargarFronterasEnMemoria();
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
//            if (!jugadorActual.getToken().equals(token)) {
//                sendToPlayer(token, "No es tu turno");
//                return;
//            }

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
                    break;
                case "atacarRQ":
                    handleAtacar(jugadorActual, json);
                    break;
                case "meAtacanRQ":
                    handleMeAtacan(jugadorActual,json);
                    break;
                case "conquistaRQ":
                    handleMoverTropas(jugadorActual,json);
                    break;
                case "moverTropasRQ":
                    handleMoverTropas(jugadorActual,json);
                    break;
                default:
                    sendToPlayer(token, "Acción no válida");
            }
        } catch (JsonProcessingException e) {
            System.err.println("Error al parsear JSON: " + e.getMessage());
        }
    }

    private void handleMoverTropas(JugadorJuego jugadorActual, JsonNode json) {
        MoverTropasRQ rq = objectMapper.convertValue(json, MoverTropasRQ.class);
        String desde = null;
        String hasta = null;

        // 1) Comprobar que controla ambos países
        for (String pais : jugadorActual.getPaisesControlados().keySet()) {
            if (rq.getPaisOrigen().equals(pais)) {
                desde = pais;
            } else if (rq.getPaisDestino().equals(pais)) {
                hasta = pais;
            }
            if (desde != null && hasta != null) break;
        }
        if (desde == null || hasta == null) {
            sendError(jugadorActual, "Debes controlar tanto el país de origen como el de destino");
        }

        // 2) Validar ruta terrestre
        if (!validarRutaTerrestre(jugadorActual.getToken(), desde, hasta, new HashSet<>())) {
            sendError(jugadorActual, "No hay ruta terrestre válida entre " + desde + " y " + hasta);
            return;
        }

        int tropasOrigen = jugadorActual.getPaisesControlados().get(desde);
        int tropasMover  = rq.getNumTropas();
        int tropasDestino = jugadorActual.getPaisesControlados().getOrDefault(hasta, 0);

        // 3) Verificar que no deje <1 tropa en el origen
        if (tropasOrigen - tropasMover < 1) {
            sendError(jugadorActual, "Las tropas del país de origen no pueden ser menos de 1");
            return;
        }

        // 4) Actualizar conteos
        jugadorActual.getPaisesControlados().put(desde, tropasOrigen - tropasMover);
        jugadorActual.getPaisesControlados().put(hasta, tropasDestino + tropasMover);

        // 5) Enviar estado de la partida
        PartidaRS rs = new PartidaRS();
        PartidaJuego partidaJuego = getPartidaJuego();
        partidaJuego.setJugadores(jugadoresEnPartida);
        rs.setCode(200);
        rs.setResponse("PartidaBC");
        rs.setPartida(partidaJuego);
        broadcast(toJson(rs));
    }

    private void sendError(JugadorJuego jugador, String mensaje) {
        ErrorRS ers = new ErrorRS();
        ers.setCode(500);
        ers.setMesage(mensaje);
        ers.setResponse("ErrorRS");
        String json = toJson(ers);
        System.out.println(json);
        sendToPlayer(jugador.getToken(), json);
    }


    private void handleMeAtacan(JugadorJuego jugadorActual, JsonNode json) {
        MeAtacanRQ rq = objectMapper.convertValue(json, MeAtacanRQ.class);
        ResultadoAtaqueRS rrs = new ResultadoAtaqueRS();
        JugadorJuego atacante = null;

        // Buscar al atacante real desde la lista de jugadores
        for (JugadorJuego j : jugadoresEnPartida) {
            if (j.getPaisesControlados().containsKey(rq.getPaisAtacante())) {
                atacante = j;
                break;
            }
        }

        if (atacante == null) {
            System.err.println("No se encontró atacante para el país: " + rq.getPaisAtacante());
            return;
        }

        // Generar dados ataque
        List<Integer> dadosAtaque = new ArrayList<>();
        for (int i = 0; i < rq.getNumTropasAtaque(); i++) {
            dadosAtaque.add(new Random().nextInt(6) + 1);
        }

        // Generar dados defensa
        List<Integer> dadosDefensa = new ArrayList<>();
        for (int i = 0; i < rq.getNumTropasDefensa(); i++) {
            dadosDefensa.add(new Random().nextInt(6) + 1);
        }

        Collections.sort(dadosAtaque, Collections.reverseOrder());
        Collections.sort(dadosDefensa, Collections.reverseOrder());

        int tropasPerdidasDefensor = 0;
        int tropasPerdidasAtacante = 0;

        int comparaciones = Math.min(dadosAtaque.size(), dadosDefensa.size());
        for (int i = 0; i < comparaciones; i++) {
            if (dadosDefensa.get(i) >= dadosAtaque.get(i)) {
                tropasPerdidasAtacante++;
            } else {
                tropasPerdidasDefensor++;
            }
        }

        // Actualizar tropas
        int tropasAtacanteActual = atacante.getPaisesControlados().get(rq.getPaisAtacante());
        int nuevasTropasAtacante = tropasAtacanteActual - tropasPerdidasAtacante;

        JugadorJuego defensor = null;
        for (JugadorJuego j : jugadoresEnPartida) {
            if (j.getPaisesControlados().containsKey(rq.getPaisDefensor())) {
                defensor = j;
                break;
            }
        }

        if (defensor == null) {
            System.err.println("No se encontró defensor para el país: " + rq.getPaisDefensor());
            return;
        }

        int tropasDefensaActual = defensor.getPaisesControlados().get(rq.getPaisDefensor());
        int nuevasTropasDefensor = tropasDefensaActual - tropasPerdidasDefensor;

        atacante.setTotalTropas(atacante.getTotalTropas() - tropasPerdidasAtacante);
        defensor.setTotalTropas(defensor.getTotalTropas() - tropasPerdidasDefensor);

        if (nuevasTropasDefensor <= 0) {
            // Conquista
            defensor.getPaisesControlados().remove(rq.getPaisDefensor());

            // Transferir el país al atacante con al menos 1 tropa
            atacante.getPaisesControlados().put(rq.getPaisDefensor(), 1);
            atacante.getPaisesControlados().put(rq.getPaisAtacante(), nuevasTropasAtacante - 1); // mueve 1

            territorioJugador.remove(rq.getPaisDefensor());
            territorioJugador.put(rq.getPaisDefensor(), atacante.getToken());

            HasConquistadoRQ rqc = new HasConquistadoRQ();
            rqc.setConquistado(rq.getPaisDefensor());
            rqc.setAtacante(rq.getPaisAtacante());
            rqc.setResponse("hasConquistadoRS");
            rqc.setCode(200);
            sendToPlayer(atacante.getToken(), toJson(rqc));
        } else {
            // No hubo conquista, solo se actualizan tropas
            defensor.getPaisesControlados().put(rq.getPaisDefensor(), nuevasTropasDefensor);
            atacante.getPaisesControlados().put(rq.getPaisAtacante(), nuevasTropasAtacante);
        }

        // Actualizar jugadores en lista (por referencia no es necesario, pero si clonas objetos, sí)
        for (int i = 0; i < jugadoresEnPartida.size(); i++) {
            JugadorJuego j = jugadoresEnPartida.get(i);
            if (j.getToken().equals(defensor.getToken())) {
                jugadoresEnPartida.set(i, defensor);
            } else if (j.getToken().equals(atacante.getToken())) {
                jugadoresEnPartida.set(i, atacante);
            }
        }

        // Resultado del ataque para ambos jugadores
        rrs.setDadosAtaque(dadosAtaque);
        rrs.setDadosDefensa(dadosDefensa);
        rrs.setNumTropasAtaque(rq.getNumTropasAtaque());
        rrs.setNumTropasDefensa(rq.getNumTropasDefensa());
        rrs.setPaisAtacante(rq.getPaisAtacante());
        rrs.setPaisDefensor(rq.getPaisDefensor());
        rrs.setTropasPerdidasAtacante(tropasPerdidasAtacante);
        rrs.setTropasPerdidasDefensor(tropasPerdidasDefensor);
        rrs.setResponse("resultadoAtaqueRS");
        rrs.setCode(200);

        String jsonRS = toJson(rrs);
        sendToPlayer(atacante.getToken(), jsonRS);
        sendToPlayer(defensor.getToken(), jsonRS);

        // Enviar estado actualizado a todos
        PartidaRS prs = new PartidaRS();
        PartidaJuego p = getPartidaJuego();
        p.setJugadores(jugadoresEnPartida);
        prs.setPartida(p);
        prs.setResponse("partidaBC");
        prs.setCode(200);
        broadcast(toJson(prs));

        // Verificar si hay ganador
        if (ganador()) {
            removePartidaJuego();
        }
    }



    private void handleAtacar(JugadorJuego jugadorActual, JsonNode json) {
        AtacarRQ rq = objectMapper.convertValue(json, AtacarRQ.class);
        JugadorJuego defensor = null;
        for(JugadorJuego j : jugadoresEnPartida){
            if(j.getPaisesControlados().containsKey(rq.getPaisDefensor())){
                defensor= j;
                break;
            }
        }
        if(sonTerritoriosAdyacentes(rq.getPaisAtacante(),rq.getPaisDefensor())){
            TeAtacanRS rs = new TeAtacanRS();
            rs.setResponse("teAtacanRS");
            rs.setCode(200);
            int numTropas= rq.getNumTropas();
            rs.setNumTropasAtaque(numTropas);
            rs.setPaisAtacante(rq.getPaisAtacante());
            rs.setPaisDefensor(rq.getPaisDefensor());
            System.out.println(toJson(rs));
            sendToPlayer(defensor.getToken(),toJson(rs));
        }else{
            ErrorRS errorRS = new ErrorRS();
            errorRS.setResponse("error");
            errorRS.setMesage("No son paises contiguos");
            errorRS.setCode(407);
            System.out.println(errorRS);
            System.out.println(toJson(errorRS));
            sendToPlayer(defensor.getToken(),toJson(errorRS));
        }

    }


    private void handleReforzarPais(JugadorJuego jugadorActual, JsonNode json) {
        ReforzarPaisRQ rq = objectMapper.convertValue(json, ReforzarPaisRQ.class);


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
                if(rq.getRequest().equals("reforzarPaisRQ")){
                    avanzarAlSiguienteConTropas();
                }

                int index = getCurrentPlayerIndex();
                rs.getPartida().setTurno(jugadoresEnPartida.get(index).getId());

                int c=0;
                for(JugadorJuego j : partidaActual.jugadores){
                    if(j.getTropasTurno()==0){
                        c++;
                    }
                }
                if(c==partidaActual.jugadores.size()&&rq.getRequest().equals("reforzarPaisRQ")){
                        currentPhase = Estat.REFORC_TROPES;
                        List<JugadorJuego> act = new ArrayList<>();
                        for(int i =0;i<jugadoresEnPartida.size();i++){
                            JugadorJuego j=jugadoresEnPartida.get(i);
                            j.setTotalTropas(j.getTotalTropas()+calcularTropasInicioTurno(j));
                            j.setTropasTurno(calcularTropasInicioTurno(j));
                            j.setTotalTropas(j.getTotalTropas()+calcularBonusContinentes(j));
                            j.setTropasTurno(j.getTropasTurno()+calcularBonusContinentes(j));
                            jugadoresEnPartida.set(i, j);
                        }
                       partidaActual.setJugadores(jugadoresEnPartida);

                    rs.getPartida().setFase(currentPhase);
                    rs.setPartida(partidaActual);
                    broadcast(toJson(rs));
                }
                else{
                    rs.setPartida(partidaActual);
                    broadcast(toJson(rs));
                }
                if(currentPhase==Estat.REFORC_TROPES&&jugadorActual.getTropasTurno()==0){
                    currentPhase = Estat.COMBAT;
                    partidaActual.setFase(currentPhase);
                    rs.setPartida(partidaActual);
                    broadcast(toJson(rs));
                }

            }else{
                ErrorRS rs = new ErrorRS();
                rs.setResponse("ErrorRS");
                rs.setCode(401);
                rs.setMesage("No tienes suficientes tropas disponibles para esta acción");
                PlayerSession player = players.get(rq.getToken());
                System.out.println(toJson(rs));
                player.send(toJson(rs));
            }
        }else {
            ErrorRS rs = new ErrorRS();
            rs.setResponse("ErrorRS");
            rs.setCode(402);
            rs.setMesage("El jugador no controla este pais");
            System.out.println(toJson(rs));
            sendToPlayer(jugadorActual.getToken(),toJson(rs));
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
                paisesColocaos+=j.getPaisesControlados().size();
            }

            if(paisRepository.count()==paisesColocaos){
                partidaActual.setFase(Estat.REFORC_PAIS);
            }
            partidaRS.setPartida(partidaActual);
            JugadorJuego j1 =jugadoresEnPartida.get(0);
            JugadorJuego j2 =jugadoresEnPartida.get(1);
            String json = "{\"partida\":{\"jugadores\":[{\"id\":"+j1.getId()+",\"nombre\":\"test1\",\"totalTropas\":40,\"tropasTurno\":0,\"paisesControlados\":{\"Central America\":1,\"Argentina\":1,\"Northwest Territory\":1,\"Alberta\":1,\"Eastern United States\":1,\"Quebec\":1,\"Ukraine\":1,\"North Africa\":1,\"Scandinavia\":1,\"Iceland\":1,\"Western Europe\":20,\"Venezuela\":1,\"Great Britain\":1,\"New Guinea\":1,\"Brazil\":1,\"Alaska\":1,\"Western United States\":1,\"Ontario\":1,\"Peru\":1,\"Greenland\":1,\"Indonesia\":1},\"color\":\"BLAU\",\"token\":\"215fd24b-d682-43cd-a207-5bcec9eb4484\",\"continentesControlados\":null},{\"id\":"
                    +j2.getId()+",\"nombre\":\"test3\",\"totalTropas\":40,\"tropasTurno\":0,\"paisesControlados\":{\"Ural\":1,\"Western Australia\":1,\"Afghanistan\":1,\"Northern Europe\":1,\"Siam\":1,\"Japan\":1,\"Egypt\":1,\"Madagascar\":1,\"Congo\":16,\"India\":1,\"Middle East\":1,\"Yakutsk\":1,\"Mongolia\":1,\"Irkutsk\":1,\"China\":1,\"Siberia\":1,\"Kamchatka\":1,\"South Africa\":1,\"Southern Europe\":5,\"East Africa\":1,\"Eastern Australia\":1},\"color\":\"VERMELL\",\"token\":\"d9dda8d9-2532-4c63-9ef7-00f01ee122d7\",\"continentesControlados\":null}],\"turno\":"+j1.getId()+",\"fase\":\"COMBAT\"},\"code\":200,\"response\":\"partidaBC\"}";

            broadcast(toJson(partidaRS));//toJson(partidaRS)


        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void avanzarAlSiguienteConTropas() {
        int totalJugadores = jugadoresEnPartida.size();
        int intentos = 0;

        do {
            next(); // avanza al siguiente jugador
            intentos++;
        } while (jugadoresEnPartida.get(getCurrentPlayerIndex()).getTropasTurno() <= 0 && intentos < totalJugadores);
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
            List<Pais> paises = continente.getPaisos();
            int i = 0;
            boolean controlaAhora = true;

            // Usamos while sin break
            while (i < paises.size() && controlaAhora) {
                Pais pais = paises.get(i);
                if (!jugador.getPaisesControlados().containsKey(pais.getNom())) {
                    controlaAhora = false; // marcamos que no controla el continente
                }
                i++;
            }

            if (controlaAhora) {
                boolean yaLoControlaba = jugador.getContinentesControlados().contains(continente.getId());

                if (!yaLoControlaba) {
                    bonusTotal += continente.getReforç();
                    jugador.getContinentesControlados().add(continente.getId());
                }
            } else {
                if(jugador.getContinentesControlados().contains(continente.getId())) {
                    jugador.getContinentesControlados().remove(continente.getId());
                }

            }
        }

        return bonusTotal;
    }


    private void siguienteTurno() {
        // Rotar jugadores
        PartidaJuego p = getPartidaJuego();

        if(p.getFase().equals(Estat.REFORC_TROPES)){
            currentPhase=Estat.COMBAT;
        }else if(p.getFase().equals(Estat.COMBAT)){
            currentPhase=Estat.RECOL_LOCACIO;
        }else if(p.getFase().equals(Estat.RECOL_LOCACIO)){
            next();
            currentPhase=Estat.REFORC_TROPES;
        }
        if(!ganador()) {
            PartidaJuego partidaActual = getPartidaJuego();
            partidaActual.setFase(currentPhase);
            partidaActual.setTurno(jugadoresEnPartida.get(getCurrentPlayerIndex()).getId());
            PartidaRS rs = new PartidaRS();
            rs.setCode(200);
            rs.setResponse("partidaBC");
            rs.setPartida(partidaActual);
            broadcast(toJson(rs));
        }else{

        }

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
            jug.setContinentesControlados(new HashSet<>());
            jugadorJuegos.add(jug);
        }
        return jugadorJuegos;
    }
    public void sendToPlayer(String playerId, String message) {
        PlayerSession player = players.get(playerId);
        if (player != null && player.getWebSocket().isOpen()) {
            System.out.println(message);
            player.send(message);
        } else {
            System.err.println("No se pudo enviar mensaje al jugador " + playerId + ": conexión cerrada o no existe");
        }
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
    public boolean ganador() {
        for(JugadorJuego j : jugadoresEnPartida) {
            if(j.getPaisesControlados().size()== paisRepository.count()){
                GanadorRS rs = new GanadorRS();
                rs.setResponse("ganadorRS");
                rs.setToken(j.getToken());
                rs.setId(j.getId());
                rs.setNombre(j.getNombre());
                rs.setCode(200);
                broadcast(toJson(rs));
                return true;
            }
        }
        return false;
    }
}