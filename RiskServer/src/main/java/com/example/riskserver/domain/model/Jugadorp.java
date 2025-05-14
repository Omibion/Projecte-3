package com.example.riskserver.domain.model;

import jakarta.persistence.*;

@Entity (name = "Jugador")
public class Jugadorp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column
    private int user_id;
    @Column
    private String nombre;
    @ManyToOne
    @JoinColumn(name = "partida_id", referencedColumnName = "id")
    private Partida partida;
    @Column
    private boolean estado;
    @Column
    private String colors;
    @Column
    String token;

    public Jugadorp() {
    }

    public Jugadorp(int id, String nom) {
    }

    public long getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public Partida getPartida() {
        return partida;
    }

    public void setPartida(Partida partida) {
        this.partida = partida;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }


    public String getColors() {
        return colors;
    }

    public void setColors(String colors) {
        this.colors = colors;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }


}
