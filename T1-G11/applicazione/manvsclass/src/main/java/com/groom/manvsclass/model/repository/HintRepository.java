package com.groom.manvsclass.model.repository;

import com.groom.manvsclass.model.HintEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HintRepository extends JpaRepository<HintEntity, Long> {
}
