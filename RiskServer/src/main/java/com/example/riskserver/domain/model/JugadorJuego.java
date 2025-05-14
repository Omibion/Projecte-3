package com.example.riskserver.domain.model;

import java.util.HashMap;

public class JugadorJuego {
    int id;
    String nombre;
    int totalTropas;
    int tropasTurno;
    HashMap<Pais, Integer> paisesControlados;
    String color;
}