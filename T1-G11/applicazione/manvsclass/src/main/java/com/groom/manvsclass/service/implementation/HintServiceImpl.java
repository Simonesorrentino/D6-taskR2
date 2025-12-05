package com.groom.manvsclass.service.implementation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.manvsclass.mapper.HintMapper;
import com.groom.manvsclass.model.AdminEntity;
import com.groom.manvsclass.model.ClassUTEntity;
import com.groom.manvsclass.model.HintEntity;
import com.groom.manvsclass.model.dto.Hint;
import com.groom.manvsclass.model.enums.HintTypeEnum;
import com.groom.manvsclass.model.repository.AdminRepository;
import com.groom.manvsclass.model.repository.HintRepository;
import com.groom.manvsclass.model.repository.ClassUTRepository;
import com.groom.manvsclass.service.HintService;
import com.groom.manvsclass.service.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    
    @Autowired
    private HintMapper hintMapper;

    @Value("${t1.upload-dir}")
    private String uploadDir;

    @Override
    public List<HintEntity> getHints(Map<String, String> queryParams, String jwtToken) {
        return null;
    }


    @Override
    public String createHintsFromFile(MultipartFile file, List<MultipartFile> imageFiles, String jwtToken) {

        if (jwtToken == null || jwtToken.isEmpty() || !jwtService.isJwtValid(jwtToken)) {
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Invalid JWT token");
        }
        String adminEmail = jwtService.getAdminFromJwt(jwtToken);
        if (adminEmail == null) {
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Utente non autorizzato o token JWT malformato.");
        }

        List<Hint> hintList;
        try {
            hintList = objectMapper.readValue(file.getInputStream(), new TypeReference<List<Hint>>() {});
        } catch (IOException e) {
            throw new HttpClientErrorException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formato file non valido. Assicurati che sia un JSON valido.");
        }

        if (hintList == null || hintList.isEmpty()) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Il file non contiene suggerimenti validi da caricare.");
        }

        AdminEntity adminEntity = adminRepository.findById(adminEmail).orElse(null);
        if (adminEntity == null) {
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Amministratore non trovato nel database.");
        }

        List<String> classNames = hintList.stream()
                .filter(h -> h.getType().equals(HintTypeEnum.CLASS))
                .map(h -> h.getClassUTName() != null ? h.getClassUTName() : null)
                .filter(name -> name != null && !name.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (!classNames.isEmpty()) {
            long existingClassCount = classUTRepository.countExistingClasses(classNames);
            if (existingClassCount != classNames.size()) {
                throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Classe UT non trovata.");
            }
        }

        List<ClassUTEntity> requiredClassUTs = classUTRepository.findAllById(classNames);
        Map<String, ClassUTEntity> classUtMap = requiredClassUTs.stream()
                .collect(Collectors.toMap(ClassUTEntity::getName, c -> c));

        Map<String, MultipartFile> imageMap = imageFiles.stream()
                .filter(img -> img.getOriginalFilename() != null && !img.getOriginalFilename().isEmpty())
                .collect(Collectors.toMap(MultipartFile::getOriginalFilename, img -> img));

        List<HintEntity> hintEntityList = new ArrayList<>();

        for (Hint hint : hintList) {

            HintEntity hintEntity = hintMapper.dtoToEntity(hint);

            if (hintEntity.getContent() == null || hintEntity.getContent().trim().isEmpty()) {
                throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "Campi mancanti: 'content' è obbligatorio.");
            }

            if (hintEntity.getType() == HintTypeEnum.CLASS) {
                if (hintEntity.getClassUt() == null) {
                    throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "Campi mancanti: 'classUTName' è obbligatorio per type='class'");
                }
            } else if (hintEntity.getType() == HintTypeEnum.GENERIC) {
                hintEntity.setClassUt(null);
            } else {
                throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Tipo di suggerimento non valido. Deve essere 'generic' o 'class'");
            }

            if (hint.getImageUri() != null && !hint.getImageUri().isEmpty()) {

                String identifier = hint.getImageUri();
                MultipartFile imageFile = imageMap.get(identifier);

                if (imageFile != null) {

                    try {
                        String newFileName = System.currentTimeMillis() + "_" + identifier;
                        Path filePath = Paths.get(uploadDir, newFileName);
                        imageFile.transferTo(filePath.toFile());
                        hintEntity.setImageUri(uploadDir + newFileName);

                    } catch (IOException e) {
                        log.error("Errore durante il salvataggio del file immagine {}: {}", identifier, e.getMessage());
                        throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR,
                                "Errore I/O durante il salvataggio dell'immagine: " + identifier);
                    }
                } else {
                    log.warn("URI dell'immagine '{}' trovato nel JSON, ma file non allegato.", identifier);
                }
            }

            hintEntity.setAdmin(adminEntity);

            hintEntityList.add(hintEntity);
        }

        try {
            hintRepository.saveAll(hintEntityList);
            return "Suggerimenti caricati con successo.";
        } catch (Exception e) {
            throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore interno durante il salvataggio dei suggerimenti.");
        }
    }

    @Override
    public String deleteHintByClassUT(String classUT, String jwtToken) {
        return null;
    }

    @Override
    public String deleteHintByClassUTAndOrder(String classUT, Integer order, String jwtToken) {
        return null;
    }
}
