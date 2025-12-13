package com.groom.manvsclass.model.repository.mongo;

import com.groom.manvsclass.model.ClassUT;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ClassRepositoryMongoDB extends MongoRepository<ClassUT, String> {

}
