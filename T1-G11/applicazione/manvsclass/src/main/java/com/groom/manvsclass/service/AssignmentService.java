//Modifica 08/12/2024: Creazione Service per Assignment
package com.groom.manvsclass.service;

import com.groom.manvsclass.model.Assignment;
import com.groom.manvsclass.model.Team;
import com.groom.manvsclass.model.TeamAdmin;
import com.groom.manvsclass.model.repository.AssignmentRepository;
import com.groom.manvsclass.model.repository.TeamAdminRepository;
import com.groom.manvsclass.model.repository.TeamRepository;
import com.groom.manvsclass.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CookieValue;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AssignmentService {

    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private TeamAdminRepository teamAdminRepository;
    @Autowired
    private JwtService jwtService;  // Servizio per la validazione del JWT
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private StudentService studentService;


    private boolean isOwnerOrProfessor(Long teamId, String adminEmail) {
        // Cerca tutti gli admin di quel team e verifica se tra loro c'è quello loggato con i permessi giusti
        List<TeamAdmin> admins = teamAdminRepository.findByTeamId(teamId);
        return admins.stream()
                .anyMatch(ta -> ta.getAdminEmail().equals(adminEmail) &&
                        ("Owner".equals(ta.getRole()) || "Professor".equals(ta.getRole())));
    }

    //Modifica 07/12/2024 : creazione funzione per la creazione di un assignment
    @Transactional
    public ResponseEntity<?> creaAssignment(Assignment assignment, Long idTeam, String jwt) {
        System.out.println("Creazione dell'Assignment in corso...");

        if (jwt == null || !jwtService.isJwtValid(jwt)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token JWT non valido.");
        }

        String adminUsername = jwtService.getAdminFromJwt(jwt);

        if (assignment.getTitle() == null || assignment.getTitle().isEmpty()) {
            return ResponseEntity.badRequest().body("Titolo obbligatorio.");
        }

        // CORREZIONE DATA: Usa LocalDateTime e isBefore
        if (assignment.getExpirationDate() == null || assignment.getExpirationDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("La data di scadenza deve essere futura.");
        }

        Team existingTeam = teamRepository.findById(idTeam).orElse(null);
        if (existingTeam == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Team non trovato.");
        }

        if (!isOwnerOrProfessor(idTeam, adminUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Non hai i permessi.");
        }

        // CORREZIONE RELAZIONE: Setta l'oggetto Team, non solo l'ID stringa
        assignment.setTeam(existingTeam);

        // CORREZIONE ID: Rimosso Util.generateUniqueId(). Il DB genererà l'ID (SERIAL).

        assignmentRepository.save(assignment);

        // Notifiche (logica invariata, adattata ai tipi)
        // Convertiamo la lista di stringhe (studenti) in lista di interi per il servizio notifiche
        List<Integer> idsUtenti = existingTeam.getStudenti().stream()
                .filter(s -> s.matches("\\d+")) // Filtra solo quelli numerici per sicurezza
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        if (!idsUtenti.isEmpty()) {
            notificationService.sendNotificationsToUsers(idsUtenti, "Nuovo Assignment",
                    "Nuovo compito: " + assignment.getTitle(), "Team");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body("Assignment creato con successo.");
    }

    //Modifica 08/12/2024: creazione funzioni visualizzaTeamAssignment,visualizzaAssignments e deleteAssignment
    // Funzione aggiornata per visualizzare gli Assignment di un Team
    public ResponseEntity<?> visualizzaTeamAssignment(Long idTeam, String jwt) {
        if (jwt == null || !jwtService.isJwtValid(jwt)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token non valido.");
        }

        String adminUsername = jwtService.getAdminFromJwt(jwt);

        if (!teamRepository.existsById(idTeam)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Team non trovato.");
        }

        if (!isOwnerOrProfessor(idTeam, adminUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Non hai permessi.");
        }

        List<Assignment> assignments = assignmentRepository.findByTeamId(idTeam);
        return ResponseEntity.ok(assignments);
    }

    public ResponseEntity<?> visualizzaAssignments(String jwt) {
        if (jwt == null || !jwtService.isJwtValid(jwt)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token non valido.");
        }

        String adminUsername = jwtService.getAdminFromJwt(jwt);

        // Trova tutti i team gestiti dall'admin
        List<TeamAdmin> teamAssociations = teamAdminRepository.findByAdminEmail(adminUsername);

        if (teamAssociations.isEmpty()) {
            return ResponseEntity.ok("Nessun team associato.");
        }

        List<Long> teamIds = teamAssociations.stream()
                .map(TeamAdmin::getTeamId)
                .collect(Collectors.toList());

        List<Assignment> assignments = assignmentRepository.findAllByTeamIdIn(teamIds);
        return ResponseEntity.ok(assignments);
    }

    @Transactional
    public ResponseEntity<?> deleteAssignment(Long idAssignment, String jwt) {
        if (jwt == null || !jwtService.isJwtValid(jwt)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token non valido.");
        }

        String adminUsername = jwtService.getAdminFromJwt(jwt);

        Assignment existingAssignment = assignmentRepository.findById(idAssignment).orElse(null);
        if (existingAssignment == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Assignment non trovato.");
        }

        // Recuperiamo il team tramite la relazione
        Team team = existingAssignment.getTeam();
        if (team == null) {
            // Caso limite: assignment orfano (non dovrebbe succedere col DB relazionale ben fatto)
            assignmentRepository.delete(existingAssignment);
            return ResponseEntity.ok("Assignment orfano eliminato.");
        }

        if (!isOwnerOrProfessor(team.getId(), adminUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Non hai i permessi.");
        }

        assignmentRepository.delete(existingAssignment);
        return ResponseEntity.ok().body("Assignment rimosso.");
    }
}

    