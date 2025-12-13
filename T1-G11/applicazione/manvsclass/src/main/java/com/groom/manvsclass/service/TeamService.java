/*
 * Crea - Elimina - 30/11/2024
 */

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
import org.springframework.web.bind.annotation.CookieValue;

import javax.mail.MessagingException;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private TeamAdminRepository teamAdminRepository;

    @Autowired
    private JwtService jwtService;  // Servizio per la validazione del JWT

    @Autowired
    private StudentService studentService; //Servizio per mandare query al T23

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificationService notificationService;

    private boolean isOwner(Long teamId, String adminEmail) {
        List<TeamAdmin> admins = teamAdminRepository.findByTeamId(teamId);
        return admins.stream()
                .anyMatch(ta -> ta.getAdminEmail().equals(adminEmail) && "Owner".equals(ta.getRole()));
    }

    private boolean isProfessorOrOwner(Long teamId, String adminEmail) {
        List<TeamAdmin> admins = teamAdminRepository.findByTeamId(teamId);
        return admins.stream()
                .anyMatch(ta -> ta.getAdminEmail().equals(adminEmail) &&
                        ("Owner".equals(ta.getRole()) || "Professor".equals(ta.getRole())));
    }

    //Metodo per creare un nuovo Team
    @Transactional
    public ResponseEntity<?> creaTeam(Team team, @CookieValue(name = "jwt", required = false) String jwt) {

        System.out.println("Creazione del team in corso...");

        // 1. Verifica che il token JWT sia valido
        if (jwt == null || !jwtService.isJwtValid(jwt)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token JWT non valido.");
        }

        // 2. Estrai l'username dell'Admin dal token JWT 
        String adminUsername = jwtService.getAdminFromJwt(jwt);

        if (adminUsername == null || adminUsername.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Impossibile identificare l'Admin dal token JWT.");
        }

        // 3. Controlla se il nome del team è valido
        if (team.getName() == null || team.getName().isEmpty() || team.getName().length() < 3 || team.getName().length() > 20) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Nome del team non valido. Deve essere tra 3 e 20 caratteri.");
        }

        // 4. Controlla se esiste già un team con lo stesso nome
        if (teamRepository.existsByName(team.getName())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Un team con questo nome esiste già.");
        }

        // 6. Salva il team nel database
        Team savedTeam = teamRepository.save(team);
        // 7. Crea una relazione tra Admin e Team
        TeamAdmin teamManagement = new TeamAdmin();
        teamManagement.setAdminEmail(adminUsername);    // email dell'Admin -- Ussername
        teamManagement.setTeamId(savedTeam.getId());    // ID del Team appena creato
        teamManagement.setRole("Owner");                // Ruolo (può essere parametrizzato)
        teamManagement.setIsActive(true);

        // 8. Salva la relazione nel database
        teamAdminRepository.save(teamManagement);
        // 9. Restituisci una risposta con il team creato
        return ResponseEntity.ok().body(savedTeam);
    }

    // Elimina un team dato il nome del team
    @Transactional
    public ResponseEntity<?> deleteTeam(Long idTeam, String jwt) {
        if (jwt == null || !jwtService.isJwtValid(jwt)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token JWT non valido.");
        }

        String adminUsername = jwtService.getAdminFromJwt(jwt);

        if (!teamRepository.existsById(idTeam)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Team non trovato.");
        }

        if (!isOwner(idTeam, adminUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Non hai i permessi di Owner per eliminare questo team.");
        }

        // Elimina gli assignment associati (se non hai CascadeType.ALL configurato)
        List<Assignment> assignments = assignmentRepository.findByTeamId(idTeam);
        assignmentRepository.deleteAll(assignments);

        // Elimina le associazioni Admin-Team (se necessario, altrimenti Cascade lato DB)
        List<TeamAdmin> relations = teamAdminRepository.findByTeamId(idTeam);
        teamAdminRepository.deleteAll(relations);

        // Elimina il team
        teamRepository.deleteById(idTeam);

        return ResponseEntity.status(HttpStatus.OK).body("Team eliminato con successo.");
    }

    // Modifica il nome di un team
    @Transactional
    public ResponseEntity<?> modificaNomeTeam(TeamModificationRequest request, String jwt) {
        // Conversione ID da String a Long (se il DTO ha String)
        Long idTeam = request.getIdTeam();

        String newName = request.getNewName();

        if (jwt == null || !jwtService.isJwtValid(jwt)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token JWT non valido.");
        }

        String adminUsername = jwtService.getAdminFromJwt(jwt);

        Team existingTeam = teamRepository.findById(idTeam).orElse(null);
        if (existingTeam == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Team non trovato.");
        }

        if (!isOwner(idTeam, adminUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Non hai i permessi per modificare questo team.");
        }

        if (newName == null || newName.length() > 20) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Nome team non valido.");
        }

        if (teamRepository.existsByName(newName)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Nome già in uso.");
        }

        existingTeam.setName(newName);
        teamRepository.save(existingTeam);

        return ResponseEntity.ok().body(existingTeam);
    }

    // Metodo per visualizzare i team associati a un admin specifico
    public ResponseEntity<?> visualizzaTeams(String jwt) {
        if (jwt == null || !jwtService.isJwtValid(jwt)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token JWT non valido.");
        }

        String adminUsername = jwtService.getAdminFromJwt(jwt);

        // Trova tutte le associazioni per questo admin
        List<TeamAdmin> associations = teamAdminRepository.findByAdminEmail(adminUsername);

        if (associations.isEmpty()) {
            return ResponseEntity.ok("Non sei associato ad alcun team.");
        }

        // Estrai gli ID dei team
        List<Long> teamIds = associations.stream()
                .map(TeamAdmin::getTeamId)
                .collect(Collectors.toList());

        // Recupera i team reali
        List<Team> teams = teamRepository.findAllById(teamIds);
        return ResponseEntity.ok(teams);
    }

    //Modifica 03/12/2024: Aggiunta della visualizzazione del singolo team
    public ResponseEntity<?> cercaTeam(Long idTeam, String jwt) {
        if (jwt == null || !jwtService.isJwtValid(jwt)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token JWT non valido.");
        }

        Team existingTeam = teamRepository.findById(idTeam).orElse(null);
        if (existingTeam == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Team non trovato.");
        }

        return ResponseEntity.ok().body(existingTeam);
    }

    //Modifica 03/12/2024: Aggiunta dell'aggiungiStudenti
    @Transactional
    public ResponseEntity<?> aggiungiStudenti(Long idTeam, List<String> idStudenti, String jwt) {
        if (jwt == null || !jwtService.isJwtValid(jwt)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token JWT non valido.");
        }

        String adminUsername = jwtService.getAdminFromJwt(jwt);

        Team existingTeam = teamRepository.findById(idTeam).orElse(null);
        if (existingTeam == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Team non trovato.");
        }

        if (!isOwner(idTeam, adminUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Non hai permessi.");
        }

        if (idStudenti.isEmpty()) {
            return ResponseEntity.badRequest().body("Nessuno studente selezionato.");
        }

        // Filtra studenti già presenti
        List<String> nuoviStudenti = idStudenti.stream()
                .filter(id -> !existingTeam.getStudenti().contains(id))
                .collect(Collectors.toList());

        if (nuoviStudenti.isEmpty()) {
            return ResponseEntity.badRequest().body("Tutti gli studenti sono già nel team.");
        }

        existingTeam.getStudenti().addAll(nuoviStudenti);
        existingTeam.setNumStudenti(existingTeam.getStudenti().size());

        teamRepository.save(existingTeam);

        // Logica notifiche (invariata)
        ResponseEntity<?> dettagliResponse = studentService.ottieniStudentiDettagli(nuoviStudenti, jwt);
        if (dettagliResponse.getStatusCode() == HttpStatus.OK) {
            List<Map<String, Object>> dettagli = (List<Map<String, Object>>) dettagliResponse.getBody();
            List<String> emails = dettagli.stream().map(s -> (String) s.get("email")).collect(Collectors.toList());

            try {
                emailService.sendTeamAdditionNotificationToStudents(emails, existingTeam.getName());
            } catch (MessagingException e) {
                System.out.println("Errore invio email.");
            }

            // Notifiche DB
            for (String email : emails) {
                notificationService.sendNotification(email, null, "Aggiunto al Team", "Sei nel team " + existingTeam.getName(), "Team");
            }
        }

        return ResponseEntity.ok().body(existingTeam);
    }

    //Modifica 04/12/2024: Aggiunta ottieniStudentiTeam
    public ResponseEntity<?> ottieniStudentiTeam(Long idTeam, String jwt) {
        if (jwt == null || !jwtService.isJwtValid(jwt)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token non valido.");
        }

        String adminUsername = jwtService.getAdminFromJwt(jwt);

        Team existingTeam = teamRepository.findById(idTeam).orElse(null);
        if (existingTeam == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Team non trovato.");
        }

        if (!isProfessorOrOwner(idTeam, adminUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Permesso negato.");
        }

        List<String> studentiIds = existingTeam.getStudenti();
        if (studentiIds.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Nessuno studente nel team.");
        }

        return ResponseEntity.ok(studentService.ottieniStudentiDettagli(studentiIds, jwt));
    }

    // Modifica 04/12/2024: Aggiunta rimuoviStudenteTeam
    @Transactional
    public ResponseEntity<?> rimuoviStudenteTeam(Long idTeam, String idStudente, String jwt) {
        if (jwt == null || !jwtService.isJwtValid(jwt)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token non valido.");
        }

        String adminUsername = jwtService.getAdminFromJwt(jwt);
        Team existingTeam = teamRepository.findById(idTeam).orElse(null);

        if (existingTeam == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Team non trovato.");

        if (!isOwner(idTeam, adminUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Permesso negato.");
        }

        if (!existingTeam.getStudenti().contains(idStudente)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Studente non presente nel team.");
        }

        existingTeam.getStudenti().remove(idStudente);
        existingTeam.setNumStudenti(existingTeam.getStudenti().size());
        teamRepository.save(existingTeam);

        return ResponseEntity.ok().body(existingTeam);
    }


    /**
     * Restituisce il team associato allo studente.
     *
     * @param idStudente l'identificativo dello studente
     * @return il team a cui lo studente appartiene
     */
    public Team getTeamByStudentId(String idStudente) {
        // Utilizzando il metodo di query derivata
        return teamRepository.findByIdStudenti(idStudente);
    }

    // Permetti a uno studente di vedere i componenti del proprio team 
    public ResponseEntity<?> GetStudentTeam(String studentId, String jwt) {
        // 1. Verifica se l'utente ha un team 
        Team existingTeam = getTeamByStudentId(studentId);
        if (existingTeam == null) {
            //il team non esiste 
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("L'utente non è associato a un Team");
        }
        // 2. Recupera la lista degli id degli studenti dei team
        List<String> studentiIds = existingTeam.getStudenti(); //Lista di id degli studenti
        if (studentiIds == null || studentiIds.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Non ci sono studenti associati a questo team.");
        }
        // 3. Invoca il servizio T23 per ottenere i dettagli degli utenti
        return ResponseEntity.ok(studentService.ottieniStudentiDettagli(studentiIds, jwt));
    }


}





