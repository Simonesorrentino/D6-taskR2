package com.groom.manvsclass.model.repository.mongo;

import com.groom.manvsclass.model.TeamAdminMongoDB;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamAdminRepository extends MongoRepository<TeamAdminMongoDB, String> {

    // Trova tutte le associazioni per un determinato Admin
    TeamAdminMongoDB findByAdminId(String adminId);

    // Trova tutte l'associazione relativa ad un determinato Team
    TeamAdminMongoDB findByTeamId(String teamId);

    // Trova tutte le associazioni per un determinato Admin (questa è una versione più generica)
    List<TeamAdminMongoDB> findAllByAdminId(String adminId);  // metodo che restituisce tutte le associazioni per un admin
}
