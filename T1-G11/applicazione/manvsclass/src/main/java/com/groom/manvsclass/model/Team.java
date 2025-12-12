package com.groom.manvsclass.model;

import lombok.*;
import org.springframework.data.annotation.Id;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "teams")
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // SERIAL in SQL = Long in Java

    @Column(nullable = false, unique = true)
    private String name;

    @ElementCollection
    @CollectionTable(name = "team_students", joinColumns = @JoinColumn(name = "team_id"))
    @Column(name = "student_id")
    private List<String> studenti = new ArrayList<>();

    @Column(name = "numero_studenti")
    private Integer numStudenti;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Assignment> assignments = new ArrayList<>();

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL)
    private List<TeamAdmin> teamAdmins = new ArrayList<>();
}
