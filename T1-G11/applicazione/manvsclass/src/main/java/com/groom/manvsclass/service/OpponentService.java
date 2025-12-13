package com.groom.manvsclass.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.manvsclass.api.ApiGatewayClient;
import com.groom.manvsclass.model.Admin;
import com.groom.manvsclass.model.ClassUT;
import com.groom.manvsclass.model.Operation;
import com.groom.manvsclass.model.Opponent;
import com.groom.manvsclass.model.repository.*;
import com.groom.manvsclass.service.exception.CoverageNotFoundException;
import com.groom.manvsclass.service.exception.OpponentNotFoundException;
import com.groom.manvsclass.service.exception.ScoreNotFoundException;
import com.groom.manvsclass.util.filesystem.FileOperationUtil;
import com.groom.manvsclass.util.filesystem.download.FileDownloadUtil;
import com.groom.manvsclass.util.filesystem.upload.FileUploadResponse;
import com.groom.manvsclass.util.filesystem.upload.FileUploadUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OpponentService {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(String.valueOf(OpponentService.class));

    @Autowired
    private OperationRepository operationRepository;

    @Autowired
    private ClassUTRepository classUTRepository;

    @Autowired
    private OpponentRepository opponentRepository;

    private final UploadOpponentService uploadOpponentService;
    private final Admin userAdmin;
    private final ApiGatewayClient apiGatewayClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpponentService(OperationRepository operationRepository,
                           ClassUTRepository classRepository,
                           MongoTemplate mongoTemplate,
                           UploadOpponentService uploadOpponentService, OpponentRepository opponentRepository, ApiGatewayClient apiGatewayClient) {
        this.operationRepository = operationRepository;
        this.classUTRepository = classRepository;
        this.uploadOpponentService = uploadOpponentService;
        this.opponentRepository = opponentRepository;
        this.apiGatewayClient = apiGatewayClient;

        this.userAdmin = new Admin();
        this.userAdmin.setUsername("default");
    }

    /*
     * Restituisce la lista di classi UT disponibili nel sistema
     */
    public ResponseEntity<?> getNomiClassiUT(String jwt) {
        // 2. Recupera tutte le ClassUT dal repository e restituisce solo i nomi
        List<String> classNames = classUTRepository.findAll()
                .stream()
                .map(ClassUT::getName) // Estrae solo i nomi
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
        ClassUT classe = mapper.readValue(classUTDetails, ClassUT.class);

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
        // Cerca per ID (nome classe)
        Optional<ClassUT> classeOpt = classUTRepository.findById(name);

        if (classeOpt.isPresent()) {
            return FileDownloadUtil.downloadClassFile(classeOpt.get().getUri());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("ClasseUT " + name + " non trovata");
        }
    }

    public List<ClassUT> getAllClassUTs() {
        return classUTRepository.findAll();
    }

    public List<ClassUT> filterByDifficulty(String difficulty) {
        // Metodo custom aggiunto in ClassRepository
        return classUTRepository.findByDifficulty(difficulty);
    }

    public List<ClassUT> orderByDate() {
        // Metodo custom aggiunto in ClassRepository
        return classUTRepository.findAllByOrderByDateAsc();
    }

    public List<ClassUT> orderByName() {
        // Metodo custom aggiunto in ClassRepository
        return classUTRepository.findAllByOrderByNameAsc();
    }

    public ResponseEntity<String> modificaClasse(String name, ClassUT newContent, String jwt, HttpServletRequest request) {
        Optional<ClassUT> existingClassOpt = classUTRepository.findById(name);

        if (existingClassOpt.isPresent()) {
            ClassUT existingClass = existingClassOpt.get();

            // Aggiorna i campi
            existingClass.setDate(LocalDate.now());
            existingClass.setDifficulty(newContent.getDifficulty());
            existingClass.setDescription(newContent.getDescription());
            existingClass.setCategory(newContent.getCategory());

            // L'ID (name) non dovrebbe cambiare solitamente, ma se serve va gestito cancellando e ricreando

            classUTRepository.save(existingClass);

            // Log Operazione
            Operation op = new Operation();
            op.setOperationType(1); // 1 = Modifica
            op.setDate(java.time.LocalDateTime.now());
            op.setClassUT(existingClass);
            // op.setAdmin(...) qui dovresti settare l'admin vero recuperato dal JWT

            operationRepository.save(op);

            return new ResponseEntity<>("Aggiornamento eseguito correttamente.", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Classe non trovata.", HttpStatus.NOT_FOUND);
        }
    }

    public ResponseEntity<?> eliminaClasse(String name) {
        if (classUTRepository.existsById(name)) {
            // Elimina file fisico
            try {
                eliminaFile(name);
            } catch (Exception e) {
                logger.warning("Impossibile eliminare i file fisici per " + name);
            }

            // Log Operazione
            Operation op = new Operation();
            op.setOperationType(2); // 2 = Cancellazione
            op.setDate(java.time.LocalDateTime.now());
            // Nota: non possiamo settare la ClassUT nell'operazione se la stiamo per cancellare
            // (a meno che Operation.class_name non sia nullable o testuale)

            operationRepository.save(op);

            // Elimina dal DB (Cascade dovrebbe eliminare gli opponents se configurato in SQL,
            // altrimenti JPA potrebbe lamentarsi o lasciare orfani)
            classUTRepository.deleteById(name);

            // Chiamata esterna
            apiGatewayClient.callDeleteAllClassUTOpponents(name);

            return ResponseEntity.ok().body("Classe eliminata: " + name);
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


    public List<Opponent> getAllOpponents() {
        return opponentRepository.findAll();
    }

    public Opponent getOpponentData(String classUT, String opponentType, OpponentDifficulty opponentDifficulty) {
        return opponentRepository.findByClassUT_NameAndOpponentTypeAndDifficulty(classUT, opponentType, opponentDifficulty)
                .orElseThrow(OpponentNotFoundException::new);
    }

    public EvosuiteScore getOpponentEvosuiteScore(String classUT, String opponentType, OpponentDifficulty opponentDifficulty) {
        Opponent opponent = opponentRepository.findByClassUT_NameAndOpponentTypeAndDifficulty(classUT, opponentType, opponentDifficulty)
                .orElseThrow(ScoreNotFoundException::new);

        String jsonScore = opponent.getEvosuiteScore();
        if (jsonScore == null) throw new ScoreNotFoundException();

        try {
            return objectMapper.readValue(jsonScore, EvosuiteScore.class);
        } catch (JsonProcessingException e) {
            logger.severe("Errore parsing EvosuiteScore JSON: " + e.getMessage());
            throw new ScoreNotFoundException();
        }
    }

    public JacocoScore getOpponentJacocoScore(String classUT, String opponentType, OpponentDifficulty opponentDifficulty) {
        Opponent opponent = opponentRepository.findByClassUT_NameAndOpponentTypeAndDifficulty(classUT, opponentType, opponentDifficulty)
                .orElseThrow(ScoreNotFoundException::new);

        String jsonScore = opponent.getJacocoScore();
        if (jsonScore == null) throw new ScoreNotFoundException();

        try {
            return objectMapper.readValue(jsonScore, JacocoScore.class);
        } catch (JsonProcessingException e) {
            logger.severe("Errore parsing JacocoScore JSON: " + e.getMessage());
            throw new ScoreNotFoundException();
        }
    }

    public String getOpponentCoverage(String classUT, String opponentType, OpponentDifficulty opponentDifficulty) {
        Opponent opponent = opponentRepository.findByClassUT_NameAndOpponentTypeAndDifficulty(classUT, opponentType, opponentDifficulty)
                .orElseThrow(CoverageNotFoundException::new);

        return opponent.getCoverage();
    }
}
