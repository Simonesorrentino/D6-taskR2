package com.groom.manvsclass.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import testrobotchallenge.commons.models.opponent.OpponentDifficulty; // Assicurati di avere questo import o usa String

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "opponents")
public class Opponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "difficulty")
    @Enumerated(EnumType.STRING) // Se OpponentDifficulty è un Enum
    private OpponentDifficulty opponentDifficulty;

    @Column(name = "opponent_type")
    private String opponentType;

    @Column(name = "coverage")
    private String coverage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "jacoco_score", columnDefinition = "TEXT")
    private String jacocoScore;

    @Column(name = "evosuite_score", columnDefinition = "TEXT")
    private String evosuiteScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_name", referencedColumnName = "name", nullable = false)
    @ToString.Exclude
    private ClassUT classUT;
}
