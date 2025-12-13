/*MODIFICA (5/11/2024) - Refactoring task T1
 * Util ora si occupa di implementare i servizi ritenuti di utilità generale.
 */
package com.groom.manvsclass.util;

import com.groom.manvsclass.model.interactionMongoDB;
import com.groom.manvsclass.model.repository.mongo.InteractionRepository;
import com.groom.manvsclass.model.repository.mongo.SearchRepositoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Component
public class Util {

    @Autowired
    private InteractionRepository repo_int;

    @Autowired
    private SearchRepositoryImpl srepo;

    @Autowired
    private MongoTemplate mongoTemplate;

    // Metodo per generare un ID univoco (esempio con UUID)
    //Modifica 04/12/2024
    public static String generateUniqueId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    public List<interactionMongoDB> elencaInt() {
        return repo_int.findAll();
    }

    public List<interactionMongoDB> elencaReport() {
        return srepo.findReport();
    }

    public long likes(String name) {
        return srepo.getLikes(name);
    }

    public interactionMongoDB uploadInteraction(interactionMongoDB interazione) {
        return repo_int.save(interazione);
    }

    public int API_id() {
        Random random = new Random();
        return random.nextInt(1000000 - 0 + 1) + 0;
    }

    public String API_email(int id_u) {
        return "prova." + id_u + "@email.com";
    }

    public String newLike(String name) {
        interactionMongoDB newInteractionMongoDB = new interactionMongoDB();
        int id_u = API_id();
        String email_u = API_email(id_u);
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String data = currentDate.format(formatter);

        newInteractionMongoDB.setId_i(0);
        newInteractionMongoDB.setId(id_u);
        newInteractionMongoDB.setEmail(email_u);
        newInteractionMongoDB.setName(name);
        newInteractionMongoDB.setType(1);
        newInteractionMongoDB.setDate(data);
        repo_int.save(newInteractionMongoDB);

        return "Nuova interazione di tipo 'like' inserita per la classe: " + name;
    }

    public String newReport(String name, String commento) {
        interactionMongoDB newInteractionMongoDB = new interactionMongoDB();
        int id_u = API_id();
        String email_u = API_email(id_u);
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String data = currentDate.format(formatter);

        newInteractionMongoDB.setId_i(0);
        newInteractionMongoDB.setId(id_u);
        newInteractionMongoDB.setEmail(email_u);
        newInteractionMongoDB.setName(name);
        newInteractionMongoDB.setType(0);
        newInteractionMongoDB.setDate(data);
        newInteractionMongoDB.setCommento(commento);
        repo_int.save(newInteractionMongoDB);

        return "Nuova interazione di tipo 'report' inserita per la classe: " + name;
    }

    public interactionMongoDB eliminaInteraction(int id_i) {
        Query query = new Query();
        query.addCriteria(Criteria.where("id_i").is(id_i));
        return mongoTemplate.findAndRemove(query, interactionMongoDB.class);
    }
}