package com.groom.manvsclass.model.repository.mongo;

import com.groom.manvsclass.model.OperationMongoDB;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OperationRepository extends MongoRepository<OperationMongoDB, String> {

}
