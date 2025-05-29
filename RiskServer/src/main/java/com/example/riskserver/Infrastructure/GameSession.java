package com.example.riskserver.Infrastructure;

import com.example.riskserver.Infrastructure.persistence.*;
import com.example.riskserver.aplication.dto.*;
import com.example.riskserver.aplication.service.GameService.GameService;
import com.example.riskserver.domain.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.WebSocket;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
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
    private final OkupaJPARepository okupaRepository;
    private final ContinentJPARepository continentRepository;
    private final FronteraJPARepository fronteraRepository;
    private Estat currentPhase = Estat.WAIT;
    private final Map<WebSocket, String> connectionToToken = new ConcurrentHashMap<>();

    private List<JugadorJuego> jugadoresEnPartida;
    private final ThreadLocal<Integer> currentPlayerIndex = ThreadLocal.withInitial(() -> 0);
    private ScheduledExecutorService turnTimerExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> currentTurnTimer;
    private static final long TURN_TIMEOUT_SECONDS = 10000;


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

    public PartidaJuego getPartidaJuego() {
        return partidaJuegoThreadLocal.get();
    }

    public void setPartidaJuego(PartidaJuego partidaJuego) {
        partidaJuegoThreadLocal.set(partidaJuego);
    }

    public void removePartidaJuego() {
        partidaJuegoThreadLocal.remove();
    }
    public GameSession(String gameId, ObjectMapper objectMapper,  PartidaJpaRepository partidaRepository, JugadorpJpaRepository jugadorRepository, PaisJPARepository paisRepository, ContinentJPARepository continentRepository, FronteraJPARepository fronteraRepository, OkupaJPARepository okupaRepository) {
        this.gameId = gameId;
        this.objectMapper = objectMapper;
        this.partidaRepository = partidaRepository;
        this.jugadorRepository = jugadorRepository;
        this.paisRepository = paisRepository;
        this.continentRepository = continentRepository;
        this.fronteraRepository = fronteraRepository;
        this.okupaRepository = okupaRepository;
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
        connectionToToken.put(player.getWebSocket(), player.getPlayerId());
    }

    public void startGame(HashMap<String, PlayerSession> jugadores) {
        if (gameRunning) return;

        this.gameRunning = true;


        this.gameThread = new Thread(this::runGame);
        this.gameThread.setName("GameThread-" + gameId);
        this.gameThread.start();

    }


    private void runGame() {
        try {
            this.jugadoresEnPartida = buildJugador(gameId);

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
        } catch (InterruptedException e) {
            System.out.println("[GameThread] Interrupción recibida");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("[GameThread] Error crítico: " + e.getMessage());
            e.printStackTrace();
        } finally {
            removePartidaJuego();
            System.out.println("[GameThread] Finalizado para gameId: " + gameId);
        }
    }
    private void procesarMensaje(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            String token = json.get("token").asText();
            String request = json.get("request").asText();

            JugadorJuego jugadorActual = getJugadorActual();


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
                case "tunearJueguitoRQ":
                    handletuneitoqueteveo(jugadorActual,json);
                    break;
                default:
                    sendToPlayer(token, "Acción no válida");
            }
        } catch (JsonProcessingException e) {
            System.err.println("Error al parsear JSON: " + e.getMessage());
        }
    }

    private void handletuneitoqueteveo(JugadorJuego jugadorActual, JsonNode json) {
        PartidaJuego p = getPartidaJuego();
        List<Pais> paisL=paisRepository.findAll();
        p.getJugadores().get(0).getPaisesControlados().clear();
        p.getJugadores().get(1).getPaisesControlados().clear();
        territorioJugador.clear();
        for (int i = 0; i < paisL.size(); i++) {
            if(i<paisL.size()-2){
                territorioJugador.put(paisL.get(i).getNom(),p.getJugadores().get(0).getToken());
                p.getJugadores().get(0).getPaisesControlados().put(paisL.get(i).getNom(),10);
            }else{
                territorioJugador.put(paisL.get(i).getNom(),p.getJugadores().get(1).getToken());
                p.getJugadores().get(1).getPaisesControlados().put(paisL.get(i).getNom(),1);
            }
        }
        p.setFase(Estat.COMBAT);
        PartidaRS rs = new PartidaRS();
        rs.setPartida(p);
        rs.setCode(200);

        rs.setResponse("partidaBC");
        broadcast(toJson(rs));
    }

    private void handleMoverTropas(JugadorJuego jugadorActual, JsonNode json) {
        MoverTropasRQ rq = objectMapper.convertValue(json, MoverTropasRQ.class);
        startTurnTimer();
        String desde = null;
        String hasta = null;

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

        if (!validarRutaTerrestre(jugadorActual.getToken(), desde, hasta, new HashSet<>())) {
            sendError(jugadorActual, "No hay ruta terrestre válida entre " + desde + " y " + hasta);
            return;
        }

        int tropasOrigen = jugadorActual.getPaisesControlados().get(desde);
        int tropasMover  = rq.getNumTropas();
        int tropasDestino = jugadorActual.getPaisesControlados().getOrDefault(hasta, 0);

        if (tropasOrigen - tropasMover < 1) {
            sendError(jugadorActual, "Las tropas del país de origen no pueden ser menos de 1");
            return;
        }

        jugadorActual.getPaisesControlados().put(desde, tropasOrigen - tropasMover);
        jugadorActual.getPaisesControlados().put(hasta, tropasDestino + tropasMover);

        PartidaRS rs = new PartidaRS();
        PartidaJuego partidaJuego = getPartidaJuego();
        partidaJuego.setJugadores(jugadoresEnPartida);
        rs.setCode(200);
        rs.setResponse("partidaBC");
        rs.setPartida(partidaJuego);
        broadcast(toJson(rs));
        PersistirPartida(partidaJuego);
    }

    private void sendError(JugadorJuego jugador, String mensaje) {
        ErrorRS ers = new ErrorRS();
        ers.setCode(500);
        ers.setMesage(mensaje);
        ers.setResponse("errorRS");
        String json = toJson(ers);
        System.out.println(json);
        sendToPlayer(jugador.getToken(), json);
    }


    private void handleMeAtacan(JugadorJuego jugadorActual, JsonNode json) {
        MeAtacanRQ rq = objectMapper.convertValue(json, MeAtacanRQ.class);
        ResultadoAtaqueRS rrs = new ResultadoAtaqueRS();
        JugadorJuego atacante = null;


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
        List<Integer> dadosAtaque = new ArrayList<>();
        for (int i = 0; i < rq.getNumTropasAtaque(); i++) {
            dadosAtaque.add(new Random().nextInt(6) + 1);
        }
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

            defensor.getPaisesControlados().remove(rq.getPaisDefensor());


            atacante.getPaisesControlados().put(rq.getPaisDefensor(), 1);
            atacante.getPaisesControlados().put(rq.getPaisAtacante(), nuevasTropasAtacante - 1);
            territorioJugador.remove(rq.getPaisDefensor());
            territorioJugador.put(rq.getPaisDefensor(), atacante.getToken());
            HasConquistadoRQ rqc = new HasConquistadoRQ();
            rqc.setConquistado(rq.getPaisDefensor());
            rqc.setAtacante(rq.getPaisAtacante());
            rqc.setResponse("hasConquistadoRS");
            rqc.setCode(200);
            QuitarOkupa(defensor,rq.getPaisDefensor());
            PersistirOkupa(atacante,rq.getPaisAtacante());
            sendToPlayer(atacante.getToken(), toJson(rqc));
        } else {
            defensor.getPaisesControlados().put(rq.getPaisDefensor(), nuevasTropasDefensor);
            atacante.getPaisesControlados().put(rq.getPaisAtacante(), nuevasTropasAtacante);
        }

        for (int i = 0; i < jugadoresEnPartida.size(); i++) {
            JugadorJuego j = jugadoresEnPartida.get(i);
            if (j.getToken().equals(defensor.getToken())) {
                jugadoresEnPartida.set(i, defensor);
            } else if (j.getToken().equals(atacante.getToken())) {
                jugadoresEnPartida.set(i, atacante);
            }
        }
        if(defensor.getPaisesControlados().size()==0){
            perdedor(defensor);
        }

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

        PartidaRS prs = new PartidaRS();
        PartidaJuego p = getPartidaJuego();
        p.setJugadores(jugadoresEnPartida);
        prs.setPartida(p);
        prs.setResponse("partidaBC");
        prs.setCode(200);
        broadcast(toJson(prs));
        PersistirPartida(p);
        if (ganador()) {
            removePartidaJuego();
        }
    }



    private void handleAtacar(JugadorJuego jugadorActual, JsonNode json) {
        AtacarRQ rq = objectMapper.convertValue(json, AtacarRQ.class);
        JugadorJuego defensor = null;
        startTurnTimer();
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
            errorRS.setResponse("errorRS");
            errorRS.setMesage("No son paises contiguos");
            errorRS.setCode(407);
            System.out.println(errorRS);
            System.out.println(toJson(errorRS));
            sendToPlayer(jugadorActual.getToken(),toJson(errorRS));
        }

    }


    private void handleReforzarPais(JugadorJuego jugadorActual, JsonNode json) {
        ReforzarPaisRQ rq = objectMapper.convertValue(json, ReforzarPaisRQ.class);
        startTurnTimer();

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
                            int totalTropas=j.getTotalTropas();
                            int totalTropasTurno=j.getTropasTurno();
                            int bonus = calcularBonusContinentes(j);
                            j.setTotalTropas(totalTropas+bonus);
                            j.setTropasTurno(totalTropasTurno+bonus);
                            jugadoresEnPartida.set(i, j);
                        }
                       partidaActual.setJugadores(jugadoresEnPartida);
                    rs.getPartida().setFase(currentPhase);
                    rs.setPartida(partidaActual);
                    broadcast(toJson(rs));
                    PersistirPartida(partidaActual);
                }
                else{
                    rs.setPartida(partidaActual);
                    broadcast(toJson(rs));
                    PersistirPartida(partidaActual);
                }
                if(currentPhase==Estat.REFORC_TROPES&&jugadorActual.getTropasTurno()==0){
                    currentPhase = Estat.COMBAT;
                    partidaActual.setFase(currentPhase);
                    rs.setPartida(partidaActual);
                    broadcast(toJson(rs));
                    PersistirPartida(partidaActual);
                }



            }else{
                ErrorRS rs = new ErrorRS();
                rs.setResponse("errorRS");
                rs.setCode(401);
                rs.setMesage("No tienes suficientes tropas disponibles para esta acción");
                PlayerSession player = players.get(rq.getToken());
                System.out.println(toJson(rs));
                player.send(toJson(rs));
            }
        }else {
            ErrorRS rs = new ErrorRS();
            rs.setResponse("errorRS");
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
            startTurnTimer();
            SeleccionPaisRQ rq = objectMapper.treeToValue(data, SeleccionPaisRQ.class);
            Pais paisSeleccionado = paisRepository.findByNom(rq.getPais());


            PartidaJuego partidaActual = getPartidaJuego();
            jugadorActual.getPaisesControlados().put(paisSeleccionado.getNom(), 1);

            territorioJugador.put(paisSeleccionado.getNom(), jugadorActual.getToken());
            territorioTropas.put(paisSeleccionado.getNom(), 1);
            jugadorActual.setTropasTurno(jugadorActual.getTropasTurno() - 1);


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
            Okupa o = new Okupa(jugadorRepository.findById((int)jugadorActual.getId()),paisRepository.findByNom(rq.getPais()));
            okupaRepository.save(o);
            broadcast(toJson(partidaRS));
            PersistirPartida(partidaActual);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void avanzarAlSiguienteConTropas() {
        int totalJugadores = jugadoresEnPartida.size();
        int intentos = 0;

        do {
            next();
            intentos++;
        } while (jugadoresEnPartida.get(getCurrentPlayerIndex()).getTropasTurno() <= 0 && intentos < totalJugadores);
    }



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


    private void iniciarFaseSeleccionPaises(List<Pais> paisesDisponibles, PartidaJuego partidaJuego) {
        currentPhase = Estat.COL_LOCAR_INICIAL;
        startTurnTimer();
        PartidaRS p = new PartidaRS();
        p.setResponse("partidaBC");
        partidaJuego.setFase(currentPhase);
        p.setPartida(partidaJuego);
        p.setCode(200);

        broadcast(toJson(p));
        PersistirPartida(partidaJuego);
        broadcastFaseInicialCompletada(partidaJuego);
    }

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


    private void cargarFronterasEnMemoria() {
        List<Frontera> todasFronteras = fronteraRepository.findAll();
        Map<String, List<String>> tempCache = new HashMap<>();

        for (Frontera frontera : todasFronteras) {
            Pais pais1 = frontera.getPais1();
            Pais pais2 = frontera.getPais2();

            tempCache.computeIfAbsent(pais1.getNom(), k -> new ArrayList<>()).add(pais2.getNom());
            tempCache.computeIfAbsent(pais2.getNom(), k -> new ArrayList<>()).add(pais1.getNom());
        }

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
        connectionToToken.remove(socket);

        if (playerId != null) {
            PlayerSession disconnectedPlayer = players.remove(playerId);

            Optional<JugadorJuego> jugadorDesconectado = jugadoresEnPartida.stream()
                    .filter(j -> j.getToken().equals(playerId))
                    .findFirst();

            if (jugadorDesconectado.isPresent()) {
                JugadorJuego jugador = jugadorDesconectado.get();

                System.out.println("Jugador desconectado: " + jugador.getNombre() + " (" + playerId + ")");

                boolean eraElTurnoDelJugador = (getPartidaJuego().getTurno() == jugador.getId());


                jugador.getPaisesControlados().replaceAll((pais, tropas) -> {
                    territorioTropas.put(pais, 1);
                    return 1;
                });


                jugador.setTotalTropas(jugador.getPaisesControlados().size());
                jugador.setTropasTurno(0);


                for (int i = 0; i < jugadoresEnPartida.size(); i++) {
                    if (jugadoresEnPartida.get(i).getToken().equals(playerId)) {
                        jugadoresEnPartida.set(i, jugador);
                        break;
                    }
                }
                if (eraElTurnoDelJugador) {
                    System.out.println("Era el turno del jugador desconectado, avanzando turno...");
                    siguienteTurno();
                } else {
                    PartidaJuego partidaActual = getPartidaJuego();
                    partidaActual.setJugadores(jugadoresEnPartida);

                    PartidaRS partidaRS = new PartidaRS();
                    partidaRS.setPartida(partidaActual);
                    partidaRS.setResponse("partidaBC");
                    partidaRS.setCode(200);

                    broadcast(toJson(partidaRS));
                    PersistirPartida(partidaActual);
                }


                verificarCondicionesFinJuego();

                System.out.println("Jugador " + jugador.getNombre() + " procesado correctamente. Jugadores restantes: " + players.size());
            }
        }
    }



    private void verificarCondicionesFinJuego() {
        int jugadoresConectados = players.size();
        int jugadoresEnJuego = jugadoresEnPartida.size();

        System.out.println("Verificando condiciones: " + jugadoresConectados + " conectados, " + jugadoresEnJuego + " en juego");

        if (jugadoresConectados <= 1) {

            if (jugadoresConectados == 1) {

                JugadorJuego ganador = jugadoresEnPartida.stream()
                        .filter(j -> players.containsKey(j.getToken()))
                        .findFirst()
                        .orElse(null);

                if (ganador != null) {
                    GanadorRS rs = new GanadorRS();
                    rs.setCode(200);
                    rs.setResponse("ganadorRS");
                    rs.setNombre(ganador.getNombre());
                    rs.setId(ganador.getId());
                    rs.setToken(ganador.getToken());
                    broadcast(toJson(rs));
                }
            }


            System.out.println("Terminando juego por falta de jugadores");
            stopGame();
        } else if (jugadoresConectados == 2 && jugadoresEnJuego >= 2) {

            System.out.println("El juego continúa con " + jugadoresConectados + " jugadores");
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

    private int calcularBonusContinentes(JugadorJuego jugador) {
        int bonusTotal = 0;
        List<Continent> todosContinentes = continentRepository.findAll();


        if (jugador.getContinentesControlados() == null) {
            jugador.setContinentesControlados(new HashSet<>());
        }

        for (Continent continente : todosContinentes) {
            List<Pais> paises = continente.getPaisos();
            boolean controlaCompleto = true;


            for (Pais pais : paises) {
                if (!jugador.getPaisesControlados().containsKey(pais.getNom())) {
                    controlaCompleto = false;
                    break;
                }
            }

            if (controlaCompleto) {

                if (!jugador.getContinentesControlados().contains(continente.getId())) {
                    bonusTotal += continente.getReforç();
                    jugador.getContinentesControlados().add(continente.getId());
                    System.out.println("Jugador " + jugador.getNombre() + " GANA control del continente: " + continente.getNom() + " (Bonus: " + continente.getReforç() + ")");
                }

            } else {

                if (jugador.getContinentesControlados().contains(continente.getId())) {
                    jugador.getContinentesControlados().remove(continente.getId());
                    System.out.println("Jugador " + jugador.getNombre() + " ha PERDIDO el control del continente: " + continente.getNom());
                }
            }
        }

        System.out.println("Bonus total por continentes para " + jugador.getNombre() + ": " + bonusTotal);
        return bonusTotal;
    }

    private void siguienteTurno() {

        if (currentTurnTimer != null && !currentTurnTimer.isDone()) {
            currentTurnTimer.cancel(false);
        }

        PartidaJuego p = getPartidaJuego();

        if(p.getFase().equals(Estat.REFORC_TROPES)){
            currentPhase=Estat.COMBAT;
        }else if(p.getFase().equals(Estat.COMBAT)){
            currentPhase=Estat.RECOL_LOCACIO;
        }else if(p.getFase().equals(Estat.RECOL_LOCACIO)){
            next();
                JugadorJuego jugadorActual = getJugadorActual();
                PartidaJuego partidaActual = getPartidaJuego();
                PartidaRS rs = new PartidaRS();
                    jugadorActual.setTotalTropas(jugadorActual.getTotalTropas()+calcularTropasInicioTurno(jugadorActual));
                    jugadorActual.setTropasTurno(calcularTropasInicioTurno(jugadorActual));
                    int totalTropas=jugadorActual.getTotalTropas();
                    int totalTropasTurno=jugadorActual.getTropasTurno();
                    int bonus = calcularBonusContinentes(jugadorActual);
                    jugadorActual.setTotalTropas(totalTropas+bonus);
                    jugadorActual.setTropasTurno(totalTropasTurno+bonus);
                    jugadoresEnPartida.set(getCurrentPlayerIndex(), jugadorActual);
                    partidaActual.setJugadores(jugadoresEnPartida);
                    rs.setPartida(partidaActual);
                    broadcast(toJson(rs));
                    PersistirPartida(partidaActual);

            currentPhase=Estat.REFORC_TROPES;
        }
        if(!ganador()) {
            startTurnTimer();
            PartidaJuego partidaActual = getPartidaJuego();
            partidaActual.setFase(currentPhase);
            partidaActual.setTurno(jugadoresEnPartida.get(getCurrentPlayerIndex()).getId());
            PartidaRS rs = new PartidaRS();
            rs.setCode(200);
            rs.setResponse("partidaBC");
            rs.setPartida(partidaActual);
            broadcast(toJson(rs));
            PersistirPartida(partidaActual);
        }else{
            GanadorRS rs = new GanadorRS();
            rs.setCode(200);
            rs.setResponse("ganadorRS");
            rs.setNombre(getJugadorActual().getNombre());
            rs.setId(getJugadorActual().getId());
            rs.setToken(getJugadorActual().getToken());
            broadcast(toJson(rs));
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
        startTurnTimer();
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
                gameRunning=false;
                for(JugadorJuego jugador : jugadoresEnPartida) {
                    for(String pnom:jugador.getPaisesControlados().keySet()) {
                        QuitarOkupa(jugador,pnom);
                    }
                }
                jugadoresEnPartida.clear();
                return true;
            }
        }
        return false;
    }

    public void perdedor(JugadorJuego j){
        HasPerdidoRS rs = new HasPerdidoRS();
        rs.setResponse("hasPerdidoRS");
        rs.setCode(200);
        sendToPlayer(j.getToken(),toJson(rs));
        jugadoresEnPartida.remove(j);


    }

    private void startTurnTimer() {
        if (currentTurnTimer != null && !currentTurnTimer.isDone()) {
            currentTurnTimer.cancel(false);
        }
        PartidaJuego pj = getPartidaJuego();
        currentTurnTimer = turnTimerExecutor.schedule(() -> {
            if (gameRunning&&(pj.getFase().equals(Estat.COMBAT)||pj.getFase().equals(Estat.RECOL_LOCACIO)||pj.getFase().equals(Estat.REFORC_TROPES))) {
                System.out.println("Timeout - Pasando turno automáticamente");
                siguienteTurno();
            } else if (gameRunning&&(pj.getFase().equals(Estat.COL_LOCAR_INICIAL)||(pj.getFase().equals(Estat.REFORC_PAIS)))) {
                System.out.println("Timeout - Pasando turno automáticamente");
                next();
                pj.setTurno(jugadoresEnPartida.get(getCurrentPlayerIndex()).getId());
                PartidaRS rs = new PartidaRS();
                rs.setPartida(pj);
                rs.setResponse("partidaBC");
                rs.setCode(200);
                PersistirPartida(pj);
                broadcast(toJson(rs));
            }
        }, TURN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public void PersistirPartida(PartidaJuego pj){
        Partida p = partidaRepository.findById(Integer.parseInt(gameId));
        p.setEstado(false);
        p.setEstat_torn(pj.getFase());
        p.setTorn_playes_id((int)pj.getTurno());
        partidaRepository.save(p);
    }
    public void PersistirOkupa(JugadorJuego jugador,String pais){
        Jugadorp j = jugadorRepository.findById((int)jugador.getId());
        Okupa o = new Okupa();
        o.setJugador(j);
        o.setPais(paisRepository.findByNom(pais));
        okupaRepository.save(o);
    }
    public void QuitarOkupa(JugadorJuego j, String pais){
        Jugadorp ju = jugadorRepository.findById((int)j.getId());
        Pais p = paisRepository.findByNom(pais);
        Okupa o = new Okupa();
        o.setPais(p);
        o.setJugador(ju);
        okupaRepository.delete(o);
    }

    public void sefue(String token) {

        JugadorJuego jugadorQueSeFue = null;
        for (JugadorJuego j : jugadoresEnPartida) {
            if (j.getToken().equals(token)) {
                jugadorQueSeFue = j;
                break;
            }
        }

        if (jugadorQueSeFue == null) return;

        jugadoresEnPartida.remove(jugadorQueSeFue);


        for (JugadorJuego jugador : jugadoresEnPartida) {
            GanadorRS rsGanador = new GanadorRS();
            rsGanador.setResponse("ganadorRS");
            rsGanador.setToken(jugador.getToken());
            rsGanador.setId(jugador.getId());
            rsGanador.setNombre(jugador.getNombre());
            rsGanador.setCode(200);
            sendToPlayer(jugador.getToken(), toJson(rsGanador));
        }

        gameRunning = false;

        players.clear();
        sessionToPlayer.clear();
        connectionToToken.clear();
    }
}