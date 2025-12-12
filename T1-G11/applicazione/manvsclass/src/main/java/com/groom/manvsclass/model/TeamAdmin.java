package com.groom.manvsclass.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "team_admins")
@IdClass(TeamAdminId.class) // Collega la classe ID creata sopra
public class TeamAdmin {
    @Id
    @Column(name = "admin_email")
    private String adminEmail;

    @Id
    @Column(name = "team_id")
    private Long teamId;

    @Id
    @Column(name = "role")
    private String role; // Ruolo dell'Admin nel Team

    private String teamName; //Nome della classe

    @Column(name = "is_active")
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_email", referencedColumnName = "email", insertable = false, updatable = false)
    @ToString.Exclude
    private Admin admin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", referencedColumnName = "id", insertable = false, updatable = false)
    @ToString.Exclude
    private Team team;
}
