/*MODIFICA (5/11/2024) - Refactoring task T1
 * AdminService ora si occupa di implementare i servizi relativi all'Admin
 */
package com.groom.manvsclass.service;

import com.groom.manvsclass.model.Admin;
import com.groom.manvsclass.model.repository.AdminRepositoryMongoDB;
import com.groom.manvsclass.model.repository.ClassRepositoryMongoDB;
import com.groom.manvsclass.model.repository.OperationRepository;
import com.groom.manvsclass.model.repository.SearchRepositoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AdminServiceMongoDB {

    @Autowired
    private final SearchRepositoryImpl srepo;
    private final Admin userAdmin = new Admin("default", "default", "default", "default", "default");
    private final LocalDate today = LocalDate.now();
    @Autowired
    private JwtService jwtService;
    @Autowired
    private ClassRepositoryMongoDB repo;
    @Autowired
    private OperationRepository orepo;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private AdminRepositoryMongoDB arepo;
    //MODIFICA (15/02/2024) : Servizio di posta elettronica
    @Autowired
    private EmailService emailService;
    //MODIFICA (11/02/2024) : Controlli sul form registrazione
    @Autowired
    private PasswordEncoder myPasswordEncoder;

    public AdminServiceMongoDB(SearchRepositoryImpl srepo) {
        this.userAdmin.setUsername("default");
        this.srepo = srepo;
    }


    public ResponseEntity<List<ClassUT>> filtraClassi(String category, String jwt) {
        if (jwtService.isJwtValid(jwt)) {
            List<ClassUT> classiFiltrate = srepo.filterByCategory(category);
            return ResponseEntity.ok().body(classiFiltrate);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    public ResponseEntity<List<ClassUT>> filtraClassi(String text, String category, String jwt) {
        if (jwtService.isJwtValid(jwt)) {
            List<ClassUT> classiFiltrate = srepo.searchAndFilter(text, category);
            return ResponseEntity.ok().body(classiFiltrate);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    public ResponseEntity<?> inviteAdmins(AdminMongoDB adminMongoDB1, String jwt) {
        if (!jwtService.isJwtValid(jwt)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Attenzione, non sei loggato");
        }

        //Controlliamo che non esista nel repository un admin con la mail specificata nell'invito
        AdminMongoDB adminMongoDB = arepo.findById(adminMongoDB1.getEmail()).orElse(null);
        if (adminMongoDB != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email trovata, la persona che stai tentando di invitare è già un amministratore!");
        }

        AdminMongoDB new_adminMongoDB = new AdminMongoDB("default", "default", "default", "default", "default");
        new_adminMongoDB.setEmail(adminMongoDB1.getEmail());

        //String invitationToken = jwtService.generateToken(new_adminMongoDB);
        String invitationToken = "test";
        new_adminMongoDB.setInvitationToken(invitationToken);

        AdminMongoDB savedAdminMongoDB = arepo.save(new_adminMongoDB);
        try {
            emailService.sendInvitationToken(savedAdminMongoDB.getEmail(), savedAdminMongoDB.getInvitationToken());
            return ResponseEntity.ok().body("Invitation token inviato correttamente all'indirizzo:" + savedAdminMongoDB.getEmail());
        } catch (MessagingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore nell'invio del messaggio di posta");
        }
    }

    public ResponseEntity<?> loginWithInvitation(AdminMongoDB adminMongoDB1, String jwt) {
        if (jwtService.isJwtValid(jwt)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Attenzione, hai già un token valido!");
        }

        AdminMongoDB adminMongoDB = arepo.findById(adminMongoDB1.getEmail()).orElse(null);
        if (adminMongoDB == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email non trovata");
        }

        AdminMongoDB admin_MongoDB_invited = srepo.findAdminByInvitationToken(adminMongoDB1.getInvitationToken());
        if (!admin_MongoDB_invited.getInvitationToken().equals(adminMongoDB1.getInvitationToken())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token di invito invalido!");
        }

        adminMongoDB.setEmail(adminMongoDB1.getEmail());

        if (adminMongoDB1.getNome().length() >= 2 && adminMongoDB1.getNome().length() <= 30 && Pattern.matches("[a-zA-Z]+(\\s[a-zA-Z]+)*", adminMongoDB1.getNome())) {
            adminMongoDB.setNome(adminMongoDB1.getNome());
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Nome non valido");
        }

        if (adminMongoDB1.getCognome().length() >= 2 && adminMongoDB1.getCognome().length() <= 30 && Pattern.matches("[a-zA-Z]+(\\s?[a-zA-Z]+\\'?)*", adminMongoDB1.getCognome())) {
            adminMongoDB.setCognome(adminMongoDB1.getCognome());
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cognome non valido");
        }

        if (adminMongoDB1.getUsername().length() >= 2 && adminMongoDB1.getUsername().length() <= 30 && Pattern.matches(".*_invited$", adminMongoDB1.getUsername())) {
            adminMongoDB.setUsername(adminMongoDB1.getUsername());
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username non valido, deve rispettare il seguente formato: [username di lunghezza compresa tra 2 e 30 caratteri]_invited");
        }

        Matcher m = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,16}$").matcher(adminMongoDB1.getPassword());
        if (adminMongoDB1.getPassword().length() > 16 || adminMongoDB1.getPassword().length() < 8 || !m.matches()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Password non valida! La password deve contenere almeno una lettera maiuscola, una minuscola, un numero ed un carattere speciale e deve essere lunga tra gli 8 e i 16 caratteri");
        }

        String crypted = myPasswordEncoder.encode(adminMongoDB1.getPassword());
        adminMongoDB1.setPassword(crypted);
        adminMongoDB.setPassword(adminMongoDB1.getPassword());

        adminMongoDB1.setInvitationToken(null);
        adminMongoDB.setInvitationToken(adminMongoDB1.getInvitationToken());

        AdminMongoDB savedAdminMongoDB = arepo.save(adminMongoDB);
        return ResponseEntity.ok().body(savedAdminMongoDB);
    }

    public ResponseEntity<AdminMongoDB> getAdminByUsername(String username, String jwt) {
        if (jwtService.isJwtValid(jwt)) {

            System.out.println("Token valido, può ricercare admin per username (/admins/{username})");
            AdminMongoDB adminMongoDB = srepo.findAdminByUsername(username);
            if (adminMongoDB != null) {

                System.out.println("Operazione avvenuta con successo (/admins/{username})");
                return ResponseEntity.ok().body(adminMongoDB);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // Ritorna 404 Not Found
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null); // Ritorna 401 Unauthorized
        }
    }

}