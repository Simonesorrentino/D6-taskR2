package com.groom.manvsclass.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamAdminId implements Serializable {
    // I nomi delle variabili DEVONO essere identici a quelli nella classe Entity TeamAdmin
    private String adminEmail;
    private Long teamId;
    private String role;
}