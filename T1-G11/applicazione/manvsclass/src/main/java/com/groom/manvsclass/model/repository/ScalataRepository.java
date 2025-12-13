package com.groom.manvsclass.model.repository;

import com.groom.manvsclass.model.Scalata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/*
 * ScalataRepository extends MongoRepository with:
 * 'Scalata' as the domain type and
 * 'String' as the type of the id field
 *
 * ScalataRepository inherits several methods such as:
 * save(), findOne(), findAll(), count(), delete(), ... and
 * this allow to perform CRUD operation on the 'Scalata' objects
 */
public interface ScalataRepository extends JpaRepository<Scalata, Long> {

    // Returns all the Scalata objects with the given author
    List<Scalata> findByCreator_UsernameContaining(String username);

    // Returns all the Scalata objects with the given rounds
    List<Scalata> findByNumberRounds(int numberOfRounds);

    // Returns all the Scalata objects with the given name
    List<Scalata> findByNameContaining(String scalataName);

    List<Scalata> findByScalataNameContaining(String scalataName);
}
