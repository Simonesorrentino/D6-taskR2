package com.groom.manvsclass.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.manvsclass.api.ApiGatewayClient;
import com.groom.manvsclass.model.entity.AdminEntity;
import com.groom.manvsclass.model.entity.ClassUTEntity;
import com.groom.manvsclass.model.entity.OperationEntity;
import com.groom.manvsclass.model.entity.OpponentEntity;
import com.groom.manvsclass.model.repository.jpa.AdminRepository;
import com.groom.manvsclass.model.repository.jpa.ClassUTRepository;
import com.groom.manvsclass.model.repository.jpa.OperationRepository;
import com.groom.manvsclass.model.repository.jpa.OpponentRepository;
import com.groom.manvsclass.service.exception.CoverageNotFoundException;
import com.groom.manvsclass.service.exception.OpponentNotFoundException;
import com.groom.manvsclass.service.exception.ScoreNotFoundException;
import com.groom.manvsclass.util.filesystem.FileOperationUtil;
import com.groom.manvsclass.util.filesystem.download.FileDownloadUtil;
import com.groom.manvsclass.util.filesystem.upload.FileUploadResponse;
import com.groom.manvsclass.util.filesystem.upload.FileUploadUtil;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;
import testrobotchallenge.commons.models.opponent.OpponentDifficulty;
import testrobotchallenge.commons.models.score.EvosuiteScore;
import testrobotchallenge.commons.models.score.JacocoScore;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OpponentService {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(String.valueOf(OpponentService.class));
    private final MongoTemplate mongoTemplate;
    private final UploadOpponentService uploadOpponentService;
    private final AdminEntity userAdminEntity = new AdminEntity();
    private final ApiGatewayClient apiGatewayClient;

    private final ClassUTRepository classUTRepository;
    private final OpponentRepository opponentRepository;
    private final OperationRepository operationRepository;
    private final AdminRepository adminRepository;

    public OpponentService(MongoTemplate mongoTemplate,
                           UploadOpponentService uploadOpponentService,
                           ApiGatewayClient apiGatewayClient,
                           ClassUTRepository classUTRepository,
                           OpponentRepository opponentRepository,
                           OperationRepository operationRepository,
                           AdminRepository adminRepository
    ) {
        this.mongoTemplate = mongoTemplate;
        this.uploadOpponentService = uploadOpponentService;
        this.apiGatewayClient = apiGatewayClient;
        this.classUTRepository = classUTRepository;
        this.opponentRepository = opponentRepository;
        this.operationRepository = operationRepository;
        this.adminRepository = adminRepository;
    }

    /*
     * Restituisce la lista di classi UT disponibili nel sistema
     */
    public ResponseEntity<?> getNomiClassiUT(String jwt) {
        // 2. Recupera tutte le ClassUT dal repository e restituisce solo i nomi
        List<String> classNames = classUTRepository.findAll()
                .stream()
                .map(ClassUTEntity::getName) // Estrae solo i nomi
                .collect(Collectors.toList());

        // 3. Ritorna i nomi delle classi con lo status HTTP 200 (OK)
        return ResponseEntity.ok(classNames);
    }

    public ResponseEntity<FileUploadResponse> uploadOpponent(
            MultipartFile classUTFile,
            String classUTDetails,
            MultipartFile robotTestsZip) throws IOException {

        FileUploadResponse response = new FileUploadResponse();

        // Verifica che il file della classe sia stato ricevuto
        if (classUTFile == null || classUTFile.isEmpty()) {
            response.setErrorMessage("Errore: file della classe non ricevuto o vuoto.");
            return ResponseEntity.badRequest().body(response);
        }

        // Parsing dei dettagli della classe
        ObjectMapper mapper = new ObjectMapper();
        ClassUTEntity classe = mapper.readValue(classUTDetails, ClassUTEntity.class);

        // Nome del file e dimensione
        String classUTFileName = StringUtils.cleanPath(Objects.requireNonNull(classUTFile.getOriginalFilename()));
        long size = classUTFile.getSize();

        System.out.println("Salvataggio di " + classUTFileName + " nel filesystem condiviso");

        // Salvataggio del file della classe e robot associati
        FileUploadUtil.saveCLassFile(classUTFileName, classe.getName(), classUTFile);
        uploadOpponentService.saveOpponentsFromZip(classUTFileName, classe.getName(), classUTFile, robotTestsZip);

        // Popola la risposta
        response.setFileName(classUTFileName);
        response.setSize(size);
        response.setDownloadUri("/downloadFile");

        // Imposta i metadati della classe
        classe.setUri(String.format("%s/%s/%s/%s",
                UploadOpponentService.VOLUME_T0_BASE_PATH,
                UploadOpponentService.UNMODIFIED_SRC,
                classe.getName(),
                classUTFileName));

        classe.setDate(LocalDate.now());

        classUTRepository.save(classe);

        System.out.println("Operazione completata con successo (uploadTest)");

        return ResponseEntity.ok(response);
    }


    public ResponseEntity<?> downloadClasse(@PathVariable("name") String name) throws Exception {

        System.out.println("/downloadFile/{name} (HomeController) - name: " + name);
        System.out.println("test");
        try {
            List<ClassUTEntity> classe = classUTRepository.findByNameLike(name);
            System.out.println("File download:");
            System.out.println(classe.get(0).getUri());
            ResponseEntity file = FileDownloadUtil.downloadClassFile(classe.get(0).getUri());
            return file;
        } catch (Exception e) {
            System.out.println("Classe UT non trovata");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("ClasseUT " + name + " non trovata");
        }
    }

    public List<ClassUTEntity> getAllClassUTs() {
        return classUTRepository.findAll();
    }

    public List<ClassUTEntity> filterByDifficulty(String difficulty) {
        return classUTRepository.findByDifficulty(difficulty);
    }

    public List<ClassUTEntity> orderByDate() {
        return classUTRepository.findAllByOrderByDateAsc();
    }

    public List<ClassUTEntity> orderByName() {
        return classUTRepository.findAllByOrderByNameAsc();
    }

    public ResponseEntity<String> modificaClasse(String name, ClassUTEntity newContent, String jwt, HttpServletRequest request) {
        System.out.println("Token valido, può aggiornare informazioni inerenti le classi (update/{name})");
        Query query = new Query();
        query.addCriteria(Criteria.where("name").is(name));
        Update update = new Update().set("name", newContent.getName())
                .set("date", newContent.getDate())
                .set("difficulty", newContent.getDifficulty())
                .set("description", newContent.getDescription())
                .set("category", newContent.getCategory());
        long modifiedCount = mongoTemplate.updateFirst(query, update, ClassUTEntity.class).getModifiedCount();

        if (modifiedCount > 0) {
            LocalDate currentDate = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String data = currentDate.format(formatter);
            userAdminEntity.setName("default");
            userAdminEntity.setUsername("default");
            userAdminEntity.setPassword("default");
            userAdminEntity.setSurname("default");
            OperationEntity operationEntity1 = new OperationEntity((int) operationRepository.count(), userAdminEntity, newContent, 1, data);
            operationRepository.save(operationEntity1);
            return new ResponseEntity<>("Aggiornamento eseguito correttamente.", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Nessuna classe trovata o nessuna modifica effettuata.", HttpStatus.NOT_FOUND);
        }
    }

    public ResponseEntity<?> eliminaClasse(String name) {
        Query query = new Query();
        query.addCriteria(Criteria.where("name").is(name));
        eliminaFile(name);
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String data = currentDate.format(formatter);

        AdminEntity adminEntity = adminRepository.findByUsername("userAdmin");
        ClassUTEntity classUTEntity = classUTRepository.findById(name).get();

        OperationEntity operationEntity1 = new OperationEntity((int) operationRepository.count(), adminEntity, classUTEntity, 2, data);
        operationRepository.save(operationEntity1);
        ClassUTEntity deletedClass = mongoTemplate.findAndRemove(query, ClassUTEntity.class);

        Query query2 = new Query();
        query2.addCriteria(Criteria.where("classUT").is(name));
        mongoTemplate.findAndRemove(query, OpponentEntity.class);

        apiGatewayClient.callDeleteAllClassUTOpponents(name);
        if (deletedClass != null) {
            return ResponseEntity.ok().body(deletedClass);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Classe non trovata");
        }
    }

    public void eliminaFile(String fileName) {
        File directory = new File(String.format("%s/%s", UploadOpponentService.VOLUME_T0_BASE_PATH, fileName));
        File directoryUnmodifiedSrc = new File(String.format("%s/%s/%s", UploadOpponentService.VOLUME_T0_BASE_PATH, UploadOpponentService.UNMODIFIED_SRC, fileName));

        System.out.println("name: " + fileName);
        if (directory.exists() && directory.isDirectory()) {
            try {
                FileOperationUtil.deleteDirectoryRecursively(directory.toPath());
                FileOperationUtil.deleteDirectoryRecursively(directoryUnmodifiedSrc.toPath());
                logger.info("Cartella eliminata con successo (/deleteFile/{fileName})");
            } catch (IOException e) {
                throw new RuntimeException("Impossibile eliminare la cartella.");
            }
        } else {
            throw new RuntimeException("Cartella non trovata.");
        }
    }


    public List<OpponentEntity> getAllOpponents() {
        return opponentRepository.findAll();
    }

    public OpponentEntity getOpponentData(String classUT, String opponentType, OpponentDifficulty opponentDifficulty) {
        Optional<OpponentEntity> opponent = opponentRepository.findByClassUt_NameAndOpponentTypeAndOpponentDifficulty(classUT, opponentType, opponentDifficulty);
        if (opponent.isEmpty())
            throw new OpponentNotFoundException();

        return opponent.get();
    }

    public EvosuiteScore getOpponentEvosuiteScore(String classUT, String opponentType, OpponentDifficulty opponentDifficulty) {
        Optional<EvosuiteScore> score = opponentRepository.findEvosuiteScore(classUT,
                opponentType, opponentDifficulty);

        if (score.isEmpty())
            throw new ScoreNotFoundException();

        return score.get();
    }

    public JacocoScore getOpponentJacocoScore(String classUT, String opponentType, OpponentDifficulty opponentDifficulty) {
        Optional<JacocoScore> score = opponentRepository.findJacocoScore(classUT,
                opponentType, opponentDifficulty);

        if (score.isEmpty())
            throw new ScoreNotFoundException();

        return score.get();
    }

    public String getOpponentCoverage(String classUT, String opponentType, OpponentDifficulty opponentDifficulty) {
        Optional<String> coverage = opponentRepository.findCoverage(classUT,
                opponentType, opponentDifficulty);

        if (coverage.isEmpty())
            throw new CoverageNotFoundException();

        return coverage.get();
    }
}
