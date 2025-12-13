//Modifica 07/12/2024: Creazione AssignmentRepository

package com.groom.manvsclass.model.repository;

import com.groom.manvsclass.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByTeamId(Long idTeam);

    List<Assignment> findAllByTeamIdIn(List<Long> teamIds);

}
