package com.groom.manvsclass.model.repository.mongo;

import com.groom.manvsclass.model.interactionMongoDB;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InteractionRepository extends MongoRepository<interactionMongoDB, String> {

}
