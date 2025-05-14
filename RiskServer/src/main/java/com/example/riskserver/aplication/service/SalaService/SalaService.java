package com.example.riskserver.aplication.service.SalaService;

import com.example.riskserver.Infrastructure.persistence.JugadorpJpaRepository;
import com.example.riskserver.Infrastructure.persistence.PartidaJpaRepository;
import com.example.riskserver.Infrastructure.persistence.SalasJpaRepository;
import com.example.riskserver.Infrastructure.persistence.UserJpaRepository;
import com.example.riskserver.aplication.service.SalaService.builder.JugadorpBuilder;
import com.example.riskserver.aplication.service.SalaService.builder.SalaBuilder;
import com.example.riskserver.domain.model.Jugadorp;
import com.example.riskserver.domain.model.Partida;
import com.example.riskserver.domain.model.Sala;
import com.example.riskserver.Infrastructure.GameManager;
import com.example.riskserver.domain.model.User;
import org.springframework.stereotype.Service;

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
    public SalaService(SalasJpaRepository salasJpaRepository, JugadorpJpaRepository jugadorpJpaRepository, UserJpaRepository userJpaRepository,
    PartidaJpaRepository partidaJpaRepository) {this.salasJpaRepository = salasJpaRepository;
    this.jugadorpJpaRepository = jugadorpJpaRepository;
    this.userJpaRepository = userJpaRepository;
    this.partidaJpaRepository = partidaJpaRepository;}
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
        // Crear y persistir partida
        Partida p = new Partida();
        p.setNom(sala.getNombre());
        p.setMax_players(sala.getMaxPlayers());

        p.setEstado(false);
        p.setToken(UUID.randomUUID().toString());
        p.setDate(new Date());
        p.setAdmin_id(user.getId());
        salasJpaRepository.save(p);
        Jugadorp j = JugadorpBuilder.build(user,p,token);
        jugadorpJpaRepository.save(j);
        List <Jugadorp> jugs = new ArrayList<>();
        jugs.add(j);

        // Crear la Sala desde la Partida
        Sala nuevaSala = SalaBuilder.build(p, jugs);

        // Crear GameSession para esta partida
        GameManager.getInstance().createGame(p.getId() + "");

        return nuevaSala;
    }

    public Sala getSala(int id) {

        Sala sala = new Sala();
        Partida p = salasJpaRepository.findById(id);
        List<Jugadorp> j = jugadorpJpaRepository.findByPartida(p);
       sala= SalaBuilder.build(p,j);
        return sala;
    }

    public Sala addUserToSala(int sala, int user, String token) {
        Partida p = salasJpaRepository.findById(sala);
        List <Jugadorp> jugadorps = salasJpaRepository.findByPartidaId(sala);
        User u = userJpaRepository.findById(user);
        Sala s = SalaBuilder.build(p, jugadorps);
        if(jugadorps.size()<3){
            Partida pa = partidaJpaRepository.findById(sala);
            Jugadorp j = JugadorpBuilder.build(u,pa,token);
            jugadorpJpaRepository.save(j);
            s.getJugadores().add(j);
        }
        return s;
    }

    public List<Jugadorp> leaveUserFromSala(int sala, int user) {
        Partida p = salasJpaRepository.findById(sala);
        List <Jugadorp> j = salasJpaRepository.findByPartidaId(sala);
        for(Jugadorp jugadorp : j){
            if(jugadorp.getUser_id()==user){
                j.remove(jugadorp);
                jugadorpJpaRepository.delete(jugadorp);
                break;

            }
        }
        if (j.size()<1){
            salasJpaRepository.delete(p);
        }
        return j;
    }

}
