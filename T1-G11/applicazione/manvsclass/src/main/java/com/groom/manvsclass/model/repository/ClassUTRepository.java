package com.groom.manvsclass.model.repository;

import com.groom.manvsclass.model.ClassUT;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassUTRepository extends JpaRepository<ClassUT, String> {

    @Query("SELECT COUNT(c) FROM class_ut c WHERE c.name IN :names")
    long countExistingClasses(@Param("names") List<String> classNames);

    // Ricerca testuale (sostituisce la regex di Mongo)
    @Query("SELECT c FROM ClassUT c WHERE lower(c.name) LIKE lower(concat('%', :text, '%')) OR lower(c.description) LIKE lower(concat('%', :text, '%'))")
    List<ClassUT> findByText(@Param("text") String text);

    // Filtro categoria (cerca dentro la stringa JSON category)
    @Query("SELECT c FROM ClassUT c WHERE c.category LIKE %:category%")
    List<ClassUT> filterByCategory(@Param("category") String category);

    // Ordinamento
    List<ClassUT> findAllByOrderByDateAsc();
    List<ClassUT> findAllByOrderByNameAsc();

    // Filtro difficoltà
    List<ClassUT> findByDifficulty(String difficulty);

    // Combinazione Ricerca + Difficoltà
    @Query("SELECT c FROM ClassUT c WHERE (lower(c.name) LIKE lower(concat('%', :text, '%')) OR lower(c.description) LIKE lower(concat('%', :text, '%'))) AND c.difficulty = :difficulty")
    List<ClassUT> searchAndFilterDifficulty(@Param("text") String text, @Param("difficulty") String difficulty);

}
