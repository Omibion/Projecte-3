package com.example.riskserver.aplication.service.SalaService;

import com.example.riskserver.Infrastructure.persistence.*;
import com.example.riskserver.aplication.service.SalaService.builder.JugadorpBuilder;
import com.example.riskserver.aplication.service.SalaService.builder.SalaBuilder;
import com.example.riskserver.domain.model.Jugadorp;
import com.example.riskserver.domain.model.Partida;
import com.example.riskserver.domain.model.Sala;
import com.example.riskserver.Infrastructure.GameManager;
import com.example.riskserver.domain.model.User;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class SalaService {
    private final SalasJpaRepository salasJpaRepository;
    private final JugadorpJpaRepository jugadorpJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final PartidaJpaRepository partidaJpaRepository;
    private final OkupaJPARepository okupaJPARepository;
    public SalaService(SalasJpaRepository salasJpaRepository, JugadorpJpaRepository jugadorpJpaRepository, UserJpaRepository userJpaRepository,
    PartidaJpaRepository partidaJpaRepository,OkupaJPARepository okupaJPARepository) {this.salasJpaRepository = salasJpaRepository;
    this.jugadorpJpaRepository = jugadorpJpaRepository;
    this.userJpaRepository = userJpaRepository;
    this.partidaJpaRepository = partidaJpaRepository;
    this.okupaJPARepository = okupaJPARepository;}
    public List<Sala> getAllSalas(){
        List<Sala> salas = new ArrayList<Sala>();
        List <Partida> p = salasJpaRepository.findByEstado(false);
        List<Jugadorp> jugadors = new ArrayList<Jugadorp>();
        for(Partida partida : p){
            Sala s = new Sala();
            jugadors = salasJpaRepository.findByPartidaId(partida.getId());
            s = SalaBuilder.build(partida, jugadors);
            salas.add(s);
        }
        return salas;
    }


    public Sala createSala(Sala sala, User user, String token) {
        Partida p = new Partida();
        p.setNom(sala.getNombre());
        p.setMax_players(sala.getMaxPlayers());

        p.setEstado(false);
        p.setToken(UUID.randomUUID().toString());
        p.setDate(new Date());
        p.setAdmin_id(user.getId());
        salasJpaRepository.save(p);
        Jugadorp j = JugadorpBuilder.build(user,p,token, null);
        jugadorpJpaRepository.save(j);
        List <Jugadorp> jugs = new ArrayList<>();
        jugs.add(j);


        Sala nuevaSala = SalaBuilder.build(p, jugs);


        GameManager.getInstance().createGame(p.getId() + "");

        return nuevaSala;
    }

    public Sala getSala(int id) {
        Partida p = salasJpaRepository.findById(id);

        if (p == null) {
            throw new EntityNotFoundException("No se encontró la partida con id " + id);
        }

        List<Jugadorp> j = jugadorpJpaRepository.findByPartida(p);
        return SalaBuilder.build(p, j);
    }


    public Sala addUserToSala(int sala, int user, String token) {
        Partida p = salasJpaRepository.findById(sala);
        List <Jugadorp> jugadorps = salasJpaRepository.findByPartidaId(sala);
        User u = userJpaRepository.findById(user);
        Sala s = SalaBuilder.build(p, jugadorps);
        if(jugadorps.size()<3){
            Partida pa = partidaJpaRepository.findById(sala);
            Jugadorp j = JugadorpBuilder.build(u,pa,token,null);
            jugadorpJpaRepository.save(j);
            s.getJugadores().add(j);
        }
        return s;
    }

    @Transactional
    public List<Jugadorp> leaveUserFromSala(int salaId, int userId) {

        Partida partida = salasJpaRepository.findById(salaId);
        List<Jugadorp> jugadores = salasJpaRepository.findByPartidaId(salaId);

        Jugadorp jugadorASacar = null;
        for (Jugadorp jp : jugadores) {
            if (jp.getUser_id() == userId) {
                jugadorASacar = jp;
                break;
            }
        }
        if (jugadorASacar != null) {
            okupaJPARepository.deleteByJugadorId(jugadorASacar.getId());
            jugadores.remove(jugadorASacar);
            jugadorpJpaRepository.delete(jugadorASacar);
        }
        if (jugadores.isEmpty()) {
            salasJpaRepository.delete(partida);
        }
        return jugadores;
    }
}
