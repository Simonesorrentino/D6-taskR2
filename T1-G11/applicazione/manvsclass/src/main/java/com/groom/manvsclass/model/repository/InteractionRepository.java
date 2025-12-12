package com.groom.manvsclass.model.repository;

import com.groom.manvsclass.model.Interaction;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InteractionRepository extends MongoRepository<Interaction, String> {

}
