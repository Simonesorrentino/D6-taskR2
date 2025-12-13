/*MODIFICA (5/11/2024) - Refactoring task T1
 * ScalataService ora si occupa di implementare i servizi relativi alla modalità scalata
 */
package com.groom.manvsclass.service;

import com.groom.manvsclass.model.entity.ScalataEntity;
import com.groom.manvsclass.model.repository.jpa.ScalataRepository;
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
    private JwtService jwtService;
    @Autowired
    private ScalataRepository scalataRepository;

    public ResponseEntity<?> uploadScalata(ScalataEntity scalataEntity, String jwt) {
        if (!jwtService.isJwtValid(jwt)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("(POST /configureScalata) Attenzione, non sei loggato!");
        }

        ScalataEntity new_scalataEntity = new ScalataEntity();
        new_scalataEntity.setUsername(new_scalataEntity.getUsername());
        new_scalataEntity.setScalataName(new_scalataEntity.getScalataName());
        new_scalataEntity.setScalataDescription(new_scalataEntity.getScalataDescription());
        new_scalataEntity.setNumberOfRounds(new_scalataEntity.getNumberOfRounds());
        new_scalataEntity.setSelectedClasses(new_scalataEntity.getSelectedClasses());

        scalataRepository.save(new_scalataEntity);
        return ResponseEntity.ok().body(new_scalataEntity);
    }

    public ResponseEntity<?> listScalate() {
        List<ScalataEntity> scalate = scalataRepository.findAll();
        return new ResponseEntity<>(scalate, HttpStatus.OK);
    }

    public ResponseEntity<?> deleteScalataByName(String scalataName, String jwt) {
        if (!jwtService.isJwtValid(jwt)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("(DELETE /delete_scalata/{scalataName}) Attenzione, non sei loggato!");
        }

        List<ScalataEntity> scalataEntity = scalataRepository.findByScalataNameContainingIgnoreCase(scalataName);
        if (scalataEntity.isEmpty()) {
            return new ResponseEntity<>("Scalata con nome: " + scalataName + " non trovata", HttpStatus.NOT_FOUND);
        } else {
            scalataRepository.delete(scalataEntity.get(0));
            return new ResponseEntity<>("Scalata con nome: " + scalataName + " rimossa", HttpStatus.OK);
        }
    }

    public ResponseEntity<?> retrieveScalataByName(String scalataName) {
        List<ScalataEntity> scalataEntity = scalataRepository.findByScalataNameContainingIgnoreCase(scalataName);
        if (scalataEntity.isEmpty()) {
            return new ResponseEntity<>("Scalata with name: " + scalataName + " not found", HttpStatus.NOT_FOUND);
        } else {
            return new ResponseEntity<>(scalataEntity, HttpStatus.OK);
        }
    }
}