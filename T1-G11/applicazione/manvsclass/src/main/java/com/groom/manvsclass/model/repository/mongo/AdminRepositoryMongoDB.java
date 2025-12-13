package com.groom.manvsclass.model.repository.mongo;

import com.groom.manvsclass.model.AdminMongoDB;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface AdminRepositoryMongoDB extends MongoRepository<AdminMongoDB, String> {
    long count();  // Questo metodo conta automaticamente tutti i documenti nella collezione `Admin`
}
