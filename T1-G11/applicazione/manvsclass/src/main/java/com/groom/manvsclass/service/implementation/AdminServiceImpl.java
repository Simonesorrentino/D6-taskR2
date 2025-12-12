package com.groom.manvsclass.service.implementation;


import com.groom.manvsclass.model.Admin;
import com.groom.manvsclass.model.repository.AdminRepository;
import com.groom.manvsclass.model.repository.SearchRepositoryImpl;
import com.groom.manvsclass.service.AdminService;
import com.groom.manvsclass.service.EmailService;
import com.groom.manvsclass.service.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;
    private final JwtService jwtService;
    private final PasswordEncoder myPasswordEncoder;
    private final EmailService emailService;
    private final SearchRepositoryImpl searchRepository;

    @Autowired
    public AdminServiceImpl(
            AdminRepository adminRepository,
            JwtService jwtService,
            PasswordEncoder myPasswordEncoder,
            EmailService emailService,
            SearchRepositoryImpl searchRepository) {
        this.adminRepository = adminRepository;
        this.jwtService = jwtService;
        this.myPasswordEncoder = myPasswordEncoder;
        this.emailService = emailService;
        this.searchRepository = searchRepository;
    }


    @Override
    public ResponseEntity<Admin> getAdminByUsername(String username, String jwt) {
        if (jwtService.isJwtValid(jwt)) {

            log.debug("Token valido, può ricercare admin per username (/admins/{username})");

            Optional<Admin> adminEntity = adminRepository.findById(username);

            if (adminEntity.isPresent()) {

                log.debug("Operazione avvenuta con successo (/admins/{username})");
                return ResponseEntity.ok().body(adminEntity.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    @Override
    public ResponseEntity<?> loginWithInvitation(Admin admin, String jwt) {
        if (jwtService.isJwtValid(jwt)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Attenzione, hai già un token valido!");
        }

        Admin adminRetrieved = adminRepository.findById(admin.getEmail()).orElse(null);
        if (adminRetrieved == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email non trovata");
        }

        Admin adminInvited = adminRepository.findByInvitationToken(admin.getInvitationToken());
        if (!adminInvited.getInvitationToken().equals(admin.getInvitationToken())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token di invito invalido!");
        }

        adminRetrieved.setEmail(admin.getEmail());

        if (admin.getName().length() >= 2 && admin.getName().length() <= 30 && Pattern.matches("[a-zA-Z]+(\\s[a-zA-Z]+)*", admin.getName())) {
            adminRetrieved.setName(admin.getName());
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Nome non valido");
        }

        if (admin.getSurname().length() >= 2 && admin.getSurname().length() <= 30 && Pattern.matches("[a-zA-Z]+(\\s?[a-zA-Z]+\\'?)*", admin.getSurname())) {
            adminRetrieved.setSurname(admin.getSurname());
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cognome non valido");
        }

        if (admin.getUsername().length() >= 2 && admin.getUsername().length() <= 30 && Pattern.matches(".*_invited$", admin.getUsername())) {
            adminRetrieved.setUsername(admin.getUsername());
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username non valido, deve rispettare il seguente formato: [username di lunghezza compresa tra 2 e 30 caratteri]_invited");
        }

        Matcher m = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,16}$").matcher(admin.getPassword());
        if (admin.getPassword().length() > 16 || admin.getPassword().length() < 8 || !m.matches()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Password non valida! La password deve contenere almeno una lettera maiuscola, una minuscola, un numero ed un carattere speciale e deve essere lunga tra gli 8 e i 16 caratteri");
        }

        String crypted = myPasswordEncoder.encode(admin.getPassword());
        admin.setPassword(crypted);
        adminRetrieved.setPassword(admin.getPassword());

        admin.setInvitationToken(null);
        adminRetrieved.setInvitationToken(admin.getInvitationToken());

        Admin savedAdmin = adminRepository.save(adminRetrieved);
        return ResponseEntity.ok().body(savedAdmin);
    }

    @Override
    public ResponseEntity<?> inviteAdmins(Admin admin, String jwt) {
        if (!jwtService.isJwtValid(jwt)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Attenzione, non sei loggato");
        }

        //Controlliamo che non esista nel repository un admin con la mail specificata nell'invito
        Admin adminRetrieved = adminRepository.findById(admin.getEmail()).orElse(null);
        if (adminRetrieved != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email trovata, la persona che stai tentando di invitare è già un amministratore!");
        }

        Admin newAdmin = new Admin();
        newAdmin.setName("default");
        newAdmin.setSurname("default");
        newAdmin.setUsername("default");
        newAdmin.setEmail(admin.getEmail());
        newAdmin.setPassword("default");

        String invitationToken = JwtService.generateToken(newAdmin);
        newAdmin.setInvitationToken(invitationToken);

        Admin savedAdmin = adminRepository.save(newAdmin);
        try {
            emailService.sendInvitationToken(savedAdmin.getEmail(), savedAdmin.getInvitationToken());
            return ResponseEntity.ok().body("Invitation token inviato correttamente all'indirizzo:" + savedAdmin.getEmail());
        } catch (MessagingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore nell'invio del messaggio di posta");
        }
    }

    @Override
    public ResponseEntity<List<ClassUT>> filtraClassi(String text, String category, String jwt) {
        if (jwtService.isJwtValid(jwt)) {
            List<ClassUT> classiFiltrate = searchRepository.searchAndFilter(text, category);
            return ResponseEntity.ok().body(classiFiltrate);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    @Override
    public ResponseEntity<List<ClassUT>> filtraClassi(String category, String jwt) {
        if (jwtService.isJwtValid(jwt)) {
            List<ClassUT> classiFiltrate = searchRepository.filterByCategory(category);
            return ResponseEntity.ok().body(classiFiltrate);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }
}
