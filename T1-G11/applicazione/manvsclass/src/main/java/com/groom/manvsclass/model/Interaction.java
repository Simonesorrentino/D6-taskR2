package com.groom.manvsclass.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "interactions")
public class Interaction
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "interaction_type", nullable = false)
    private Integer interactionType;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String email; //Email utente
    private String name; //Id classe
    private long idUtente; //Id utente
    private LocalDateTime date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_email", referencedColumnName = "email", nullable = false)
    @ToString.Exclude // Evita loop infiniti nei log
    private Admin user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_name", referencedColumnName = "name", nullable = false)
    @ToString.Exclude
    private ClassUT classUT;
}