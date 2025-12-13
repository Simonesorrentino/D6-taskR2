package com.groom.manvsclass.model.repository;

import com.groom.manvsclass.model.TeamAdmin;
import com.groom.manvsclass.model.TeamAdminId; // Assicurati di aver creato questa classe
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamAdminRepository extends JpaRepository<TeamAdmin, TeamAdminId> {

    // Nota: I nomi dei metodi devono seguire i nomi dei campi nella classe Java TeamAdmin.
    // Se il campo si chiama 'adminEmail', il metodo è 'findByAdminEmail'.

    // Trova per email dell'admin (parte della chiave composta)
    List<TeamAdmin> findByAdminEmail(String adminEmail);

    // Trova per ID del team
    List<TeamAdmin> findByTeamId(Long teamId);

    // findAllByAdminId era un duplicato, findByAdminEmail basta e avanza.
}