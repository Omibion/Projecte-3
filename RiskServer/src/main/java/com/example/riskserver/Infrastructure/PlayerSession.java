package com.example.riskserver.Infrastructure;

import org.java_websocket.WebSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PlayerSession {
    private final String playerId;
    private volatile WebSocket webSocket;
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private final Thread senderThread;
    private volatile long lastActivityTime;

    public PlayerSession(String playerId, WebSocket webSocket) {
        this.playerId = playerId;
        this.webSocket = webSocket;
        this.lastActivityTime = System.currentTimeMillis();

        this.senderThread = new Thread(() -> {
            try {
                while (!Thread.interrupted() && !webSocket.isClosed()) {
                    String msg = messageQueue.take();
                    synchronized (webSocket) {
                        if (!webSocket.isClosed()) {
                            webSocket.send(msg);
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restaurar estado de interrupción
            } catch (Exception e) {
                System.err.println("Error en senderThread para jugador " + playerId + ": " + e.getMessage());
            }
        });
        this.senderThread.setName("PlayerSession-Sender-" + playerId);
        this.senderThread.start();
    }

    public String getPlayerId() {
        return playerId;
    }

    public WebSocket getWebSocket() {
        return webSocket;
    }

    public void setWebSocket(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    public long getLastActivity() {
        return lastActivityTime;
    }

    public void updateLastActivity() {
        this.lastActivityTime = System.currentTimeMillis();
    }

    public void send(String message) {
        if (message != null && !webSocket.isClosed()) {
            messageQueue.offer(message);
        }
    }

    public void close() {
        senderThread.interrupt();
        try {
            if (!webSocket.isClosed()) {
                webSocket.close();
            }
        } catch (Exception e) {
            System.err.println("Error cerrando WebSocket para jugador " + playerId + ": " + e.getMessage());
        }
    }
}