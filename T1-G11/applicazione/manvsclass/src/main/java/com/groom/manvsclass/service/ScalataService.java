/*MODIFICA (5/11/2024) - Refactoring task T1
 * ScalataService ora si occupa di implementare i servizi relativi alla modalità scalata
 */
package com.groom.manvsclass.service;

import com.groom.manvsclass.model.ScalataMongoDB;
import com.groom.manvsclass.model.repository.mongo.ScalataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScalataService {

    private static final Logger logger = LoggerFactory.getLogger(ScalataService.class);
    @Autowired
    private ScalataRepository scalata_repo;
    @Autowired
    private JwtService jwtService;

    public ResponseEntity<?> uploadScalata(ScalataMongoDB scalataMongoDB, String jwt) {
        if (!jwtService.isJwtValid(jwt)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("(POST /configureScalata) Attenzione, non sei loggato!");
        }

        ScalataMongoDB new_scalataMongoDB = new ScalataMongoDB();
        new_scalataMongoDB.setUsername(scalataMongoDB.getUsername());
        new_scalataMongoDB.setScalataName(scalataMongoDB.getScalataName());
        new_scalataMongoDB.setScalataDescription(scalataMongoDB.getScalataDescription());
        new_scalataMongoDB.setNumberOfRounds(scalataMongoDB.getNumberOfRounds());
        new_scalataMongoDB.setSelectedClasses(scalataMongoDB.getSelectedClasses());

        scalata_repo.save(new_scalataMongoDB);
        return ResponseEntity.ok().body(new_scalataMongoDB);
    }

    public ResponseEntity<?> listScalate() {
        List<ScalataMongoDB> scalate = scalata_repo.findAll();
        return new ResponseEntity<>(scalate, HttpStatus.OK);
    }

    public ResponseEntity<?> deleteScalataByName(String scalataName, String jwt) {
        if (!jwtService.isJwtValid(jwt)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("(DELETE /delete_scalata/{scalataName}) Attenzione, non sei loggato!");
        }

        List<ScalataMongoDB> scalataMongoDB = scalata_repo.findByScalataNameContaining(scalataName);
        if (scalataMongoDB.isEmpty()) {
            return new ResponseEntity<>("Scalata con nome: " + scalataName + " non trovata", HttpStatus.NOT_FOUND);
        } else {
            scalata_repo.delete(scalataMongoDB.get(0));
            return new ResponseEntity<>("Scalata con nome: " + scalataName + " rimossa", HttpStatus.OK);
        }
    }

    public ResponseEntity<?> retrieveScalataByName(String scalataName) {
        List<ScalataMongoDB> scalataMongoDB = scalata_repo.findByScalataNameContaining(scalataName);
        if (scalataMongoDB.isEmpty()) {
            return new ResponseEntity<>("Scalata with name: " + scalataName + " not found", HttpStatus.NOT_FOUND);
        } else {
            return new ResponseEntity<>(scalataMongoDB, HttpStatus.OK);
        }
    }
}