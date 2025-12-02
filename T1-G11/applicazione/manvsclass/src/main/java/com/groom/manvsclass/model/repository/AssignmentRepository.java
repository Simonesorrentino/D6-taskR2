//Modifica 07/12/2024: Creazione AssignmentRepository

package com.groom.manvsclass.model.repository;

import com.groom.manvsclass.model.AssignmentMongoDB;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AssignmentRepository extends MongoRepository<AssignmentMongoDB, String> {

    List<AssignmentMongoDB> findByTeamId(String idTeam);

    List<AssignmentMongoDB> findAllByTeamIdIn(List<String> teamIds);

}
