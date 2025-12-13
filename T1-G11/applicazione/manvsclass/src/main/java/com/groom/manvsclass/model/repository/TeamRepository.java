package com.groom.manvsclass.model.repository;

import com.groom.manvsclass.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    boolean existsByName(String name);

    // Cerca un team che contiene lo studente nella sua lista.
    // Funziona SOLO se in Team.java hai:
    // @ElementCollection
    // private List<String> idStudenti;
    @Query("SELECT t FROM Team t JOIN t.studentIds s WHERE s = :idStudente")
    Team findByIdStudenti(@Param("idStudente") String idStudente);

    // Se invece non hai mappato la lista studenti (perché complessa),
    // commenta il metodo sopra per ora.
}