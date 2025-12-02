package com.groom.manvsclass.service.implementation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.manvsclass.model.AdminEntity;
import com.groom.manvsclass.model.ClassUTEntity;
import com.groom.manvsclass.model.Hint;
import com.groom.manvsclass.model.enums.HintTypeEnum;
import com.groom.manvsclass.model.repository.AdminRepository;
import com.groom.manvsclass.model.repository.HintRepository;
import com.groom.manvsclass.model.repository.ClassUTRepository;
import com.groom.manvsclass.service.AdminService;
import com.groom.manvsclass.service.HintService;
import com.groom.manvsclass.service.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HintServiceImpl implements HintService {

    @Autowired
    private HintRepository hintRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClassUTRepository classUTRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Override
    public ResponseEntity<List<Hint>> getHints(Map<String, String> queryParams, String jwtToken) {
        return null;
    }


    @Override
    public ResponseEntity<String> createHintsFromFile(MultipartFile file, String jwtToken) {

        if (jwtToken == null || jwtToken.isEmpty() || !jwtService.isJwtValid(jwtToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token JWT non valido o mancante.");
        }
        String adminEmail = jwtService.getAdminFromJwt(jwtToken);
        if (adminEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Utente non autorizzato o token JWT malformato.");
        }

        List<Hint> hintsToSave;
        try {
            hintsToSave = objectMapper.readValue(file.getInputStream(), new TypeReference<List<Hint>>() {});
        } catch (IOException e) {
            log.error("Errore nel parsing del file JSON: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body("Formato file non valido. Assicurati che sia un JSON valido.");
        }

        if (hintsToSave == null || hintsToSave.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Il file non contiene suggerimenti validi da caricare.");
        }

        LocalDateTime now = LocalDateTime.now();

        AdminEntity adminEntity = adminRepository.findById(adminEmail).orElse(null);
        if (adminEntity == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Amministratore non trovato nel database.");
        }

        List<String> classNames = hintsToSave.stream()
                .filter(h -> h.getType().equals(HintTypeEnum.CLASS))
                .map(h -> h.getClassUt() != null ? h.getClassUt().getName() : null)
                .filter(name -> name != null && !name.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (!classNames.isEmpty()) {
            long existingClassCount = classUTRepository.countExistingClasses(classNames);
            if (existingClassCount != classNames.size()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Classe UT non trovata per uno o più suggerimenti (400 Bad Request).");
            }
        }

        List<ClassUTEntity> requiredClassUTs = classUTRepository.findAllById(classNames);
        Map<String, ClassUTEntity> classUtMap = requiredClassUTs.stream()
                .collect(Collectors.toMap(ClassUTEntity::getName, c -> c));


        for (Hint hint : hintsToSave) {

            if (hint.getContent() == null || hint.getContent().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Campi mancanti: 'content' è obbligatorio (422 Unprocessable Entity).");
            }

            if (hint.getType() == HintTypeEnum.CLASS) {

                String className = hint.getClassUt() != null ? hint.getClassUt().getName() : null;

                if (className == null || className.trim().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Campi mancanti: 'classUTName' è obbligatorio per type='class' (422 Unprocessable Entity).");
                }

                hint.setClassUt(classUtMap.get(className));

            } else if (hint.getType() == HintTypeEnum.GENERIC) {
                hint.setClassUt(null);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Tipo di suggerimento non valido. Deve essere 'generic' o 'class' (400 Bad Request).");
            }

            hint.setAdmin(adminEntity);
            hint.setCreatedAt(now);
            hint.setUpdatedAt(now);
        }

        try {
            hintRepository.saveAll(hintsToSave);
            return ResponseEntity.ok().body("Suggerimenti caricati con successo.");
        } catch (Exception e) {
            log.error("Errore durante il salvataggio dei suggerimenti: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore interno durante il salvataggio dei suggerimenti.");
        }
    }

    @Override
    public ResponseEntity<String> deleteHintByClassUT(String classUT, String jwtToken) {
        return null;
    }

    @Override
    public ResponseEntity<String> deleteHintByClassUTAndOrder(String classUT, Integer order, String jwtToken) {
        return null;
    }
}
