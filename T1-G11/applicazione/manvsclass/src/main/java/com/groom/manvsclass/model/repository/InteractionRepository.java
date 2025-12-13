package com.groom.manvsclass.model.repository;

import com.groom.manvsclass.model.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InteractionRepository extends JpaRepository<Interaction, Long> {
    // Conta i like per una classe (Type 1 = Like)
    // Assumendo che Interaction abbia un campo 'classUT' o 'className'
    @Query("SELECT count(i) FROM Interaction i WHERE i.classUT.name = :className AND i.interactionType = 1")
    long countLikes(@Param("className") String className);

    // Trova i report (Type 0 = Report)
    List<Interaction> findByInteractionType(Integer interactionType);
}
