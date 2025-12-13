package com.groom.manvsclass.service;

import lombok.Data;

//E' giusto una classe per inviare correttamente la richiesta di modifica del nome
//Da eliminare -> inutile
@Data // Usa lombok per getter/setter
public class TeamModificationRequest {
    private Long idTeam;
    private String newName;
}