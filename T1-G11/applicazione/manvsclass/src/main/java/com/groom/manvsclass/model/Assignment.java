package com.groom.manvsclass.model;

import lombok.*;
import org.springframework.data.annotation.Id;

import javax.persistence.*;
import java.time.LocalDateTime;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "assignment")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // SERIAL in SQL
    private Long id; // Usa Long per SERIAL

    // Per la visualizzazione
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id") // Nome colonna FK nello script SQL
    private Team team;

    @Column(name = "nome_team")
    private String nomeTeam;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
