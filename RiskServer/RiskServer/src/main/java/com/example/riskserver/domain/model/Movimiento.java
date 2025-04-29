package com.example.riskserver.domain.model;


    public class Movimiento {
        private String jugadorId;
        private String origen;
        private String destino;

        // Getters y setters

        public String getJugadorId() {
            return jugadorId;
        }

        public void setJugadorId(String jugadorId) {
            this.jugadorId = jugadorId;
        }

        public String getOrigen() {
            return origen;
        }

        public void setOrigen(String origen) {
            this.origen = origen;
        }

        public String getDestino() {
            return destino;
        }

        public void setDestino(String destino) {
            this.destino = destino;
        }

        @Override
        public String toString() {
            return jugadorId + ": " + origen + " -> " + destino;
        }
    }


