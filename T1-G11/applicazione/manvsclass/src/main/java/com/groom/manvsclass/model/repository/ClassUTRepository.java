package com.groom.manvsclass.model.repository;

import com.groom.manvsclass.model.ClassUTEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassUTRepository extends JpaRepository<ClassUTEntity, String> {

    @Query("SELECT COUNT(c) FROM class_ut c WHERE c.name IN :names")
    long countExistingClasses(@Param("names") List<String> classNames);

}
