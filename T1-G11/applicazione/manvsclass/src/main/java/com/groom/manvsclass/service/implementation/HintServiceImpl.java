package com.groom.manvsclass.service.implementation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.manvsclass.mapper.HintMapper;
import com.groom.manvsclass.model.entity.AdminEntity;
import com.groom.manvsclass.model.entity.ClassUTEntity;
import com.groom.manvsclass.model.entity.HintEntity;
import com.groom.manvsclass.model.dto.Hint;
import com.groom.manvsclass.model.dto.HintResponse;
import com.groom.manvsclass.model.enums.HintTypeEnum;
import com.groom.manvsclass.model.repository.jpa.AdminRepository;
import com.groom.manvsclass.model.repository.jpa.HintRepository;
import com.groom.manvsclass.model.repository.jpa.ClassUTRepository;
import com.groom.manvsclass.service.HintService;
import com.groom.manvsclass.service.JwtService;
import com.groom.manvsclass.util.HintSpecifications;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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

    @Autowired
    private Validator validator;

    @Override
    public List<HintResponse> getHints(Map<String, String> queryParams, String jwtToken) {

        jwtVerify(jwtToken);

        Specification<HintEntity> spec = HintSpecifications.withDynamicQuery(queryParams);
        List<HintEntity> results = hintRepository.findAll(spec);

        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        return hintMapper.toResponseList(results);
    }

    @Override
    public String createHintsFromFile(MultipartFile file, List<MultipartFile> imageFiles, String jwtToken) {

        jwtVerify(jwtToken);

        String adminEmail = jwtService.getAdminFromJwt(jwtToken);
        if (adminEmail == null) {
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Utente non autorizzato o token JWT malformato.");
        }

        List<Hint> hintList;
        try {
            hintList = objectMapper.readValue(file.getInputStream(), new TypeReference<List<Hint>>() {});

            //Validazione contenuto JSON
            for (Hint hint : hintList) {
                Set<ConstraintViolation<Hint>> violations = validator.validate(hint);

                if (!violations.isEmpty()) {
                    // Se ci sono violazioni, costruisci un messaggio di errore e lancia un'eccezione.
                    String violationMessage = violations.stream()
                            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                            .collect(Collectors.joining(", "));

                    throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Il contenuto del file non è valido");
                }
            }

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

            HintEntity existingHint = hintRepository.findByContentAndTypeAndClassUtName(hint.getContent(), hint.getType(), hint.getClassUTName());
            if(existingHint != null) {
                throw new HttpClientErrorException(HttpStatus.CONFLICT, "Suggerimento già esistente.");
            }

            HintEntity hintEntity = hintMapper.dtoToEntity(hint);

            if (hintEntity.getContent() == null || hintEntity.getContent().trim().isEmpty()) {
                throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "Campi mancanti: 'content' è obbligatorio.");
            }

            HintEntity hintWithTopOrder;

            if (hintEntity.getType() == HintTypeEnum.CLASS) {
                if (hintEntity.getClassUt() == null) {
                    throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "Campi mancanti: 'classUTName' è obbligatorio per type='class'");
                }
                hintWithTopOrder = hintRepository.findMaxOrderHint(hintEntity.getClassUt().getName(), hintEntity.getType()).orElse(null);
            } else if (hintEntity.getType() == HintTypeEnum.GENERIC) {
                hintEntity.setClassUt(null);
                hintWithTopOrder = hintRepository.findMaxOrderHint(null, hintEntity.getType()).orElse(null);
            } else {
                throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Tipo di suggerimento non valido. Deve essere 'generic' o 'class'");
            }

            if (hintWithTopOrder != null) {
                hintEntity.setOrder(hintWithTopOrder.getOrder() + 1);
            } else {
                hintEntity.setOrder(1);
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
                    throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,
                            "URI dell'immagine trovato nel JSON, ma file non allegato.");
                }
            }

            hintEntity.setAdmin(adminEntity);

            try {
                hintRepository.save(hintEntity);
            } catch (Exception e) {
                throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore interno durante il salvataggio dei suggerimenti.");
            }
        }

        return "Suggerimenti caricati con successo.";
    }

    @Override
    public String updateHint(MultipartFile file, List<MultipartFile> imageFiles, String jwtToken) {
        return "";
    }

    @Override
    public String deleteHintByClassUT(String classUT, String jwtToken) {
        jwtVerify(jwtToken);

        List<HintEntity> hintEntityList;
        HintTypeEnum type = classUT.equals("null") ? HintTypeEnum.GENERIC : HintTypeEnum.CLASS;

        if (type == HintTypeEnum.GENERIC) {
            hintEntityList = hintRepository.findByType(type);
        } else {
            hintEntityList = hintRepository.findByClassUtName(classUT);
        }

        if (CollectionUtils.isEmpty(hintEntityList)) {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Non ci sono suggerimenti da eliminare per " + classUT + ".");
        }

        hintRepository.deleteAll(hintEntityList);

        for (HintEntity hint : hintEntityList) {
            deleteImageFile(hint.getImageUri());
        }

        return "Suggerimenti eliminati con successo.";
    }

    @Override
    public String deleteHintByClassUTAndOrder(String classUT, Integer order, String jwtToken) {
        jwtVerify(jwtToken);

        HintEntity hintEntity;
        HintTypeEnum type = classUT.equals("null") ? HintTypeEnum.GENERIC : HintTypeEnum.CLASS;

        if (type == HintTypeEnum.GENERIC) {
            hintEntity = hintRepository.findByTypeAndOrder(type, order);
        } else {
            hintEntity = hintRepository.findByClassUtNameAndOrder(classUT, order);
        }

        if (hintEntity == null) {
            String target = type == HintTypeEnum.GENERIC ? "generico con ordine " + order : "per " + classUT + " con ordine " + order;
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Non ci sono suggerimenti da eliminare: " + target);
        }

        hintRepository.delete(hintEntity);
        deleteImageFile(hintEntity.getImageUri());

        return "Suggerimento eliminato con successo.";
    }

    private void deleteImageFile(String imageUri) {
        if (imageUri != null && !imageUri.trim().isEmpty()) {
            try {
                Path filePath = Paths.get(imageUri);

                if (imageUri.startsWith(uploadDir) && Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("File immagine eliminato con successo: {}", imageUri);

                } else if (Files.exists(filePath)) {

                    log.warn("File immagine trovato ma non nella directory di upload definita. Percorso: {}", imageUri);
                }
            } catch (IOException e) {

                log.error("Errore durante l'eliminazione del file immagine {}: {}", imageUri, e.getMessage());
            } catch (Exception e) {

                log.error("Errore generico durante l'eliminazione del file immagine {}: {}", imageUri, e.getMessage());
            }
        }
    }

    void jwtVerify(String jwtToken) {
        if (jwtToken == null || jwtToken.isEmpty() || !jwtService.isJwtValid(jwtToken)) {
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Invalid JWT token");
        }
    }
}
