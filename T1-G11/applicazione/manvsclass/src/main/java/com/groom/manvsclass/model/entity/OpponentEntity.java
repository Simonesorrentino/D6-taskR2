package com.groom.manvsclass.model.entity;

import com.groom.manvsclass.model.entity.ClassUTEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.hibernate.annotations.Type;
import testrobotchallenge.commons.models.opponent.OpponentDifficulty;
import testrobotchallenge.commons.models.score.EvosuiteScore;
import testrobotchallenge.commons.models.score.JacocoScore;

import javax.persistence.*; // Importa le annotazioni JPA standard
import java.time.Instant;

@Entity
@Table(name = "opponent")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class OpponentEntity {

    // 1. Conversione ID: Da Stringa a Chiave Primaria Sequenziale (BigInt/Long)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Usa l'identità nativa di PostgreSQL (SERIAL/BIGSERIAL)
    @Column(name = "id")
    private Long id; // ID come Long/BigInt

    // 2. Data di Creazione: JPA usa spesso @CreationTimestamp o @Temporal
    // Usiamo Instant che JPA può mappare al tipo TIMESTAMP WITH TIME ZONE
    @Column(name = "created_at")
    private Instant createdAt;

    // 3. Classe UT (Relazione o Stringa)
    // Se è una FK: @ManyToOne e @JoinColumn. Se è solo un nome/stringa, rimane VARCHAR.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_ut_name", referencedColumnName = "name")
    private ClassUTEntity classUt;

    // 4. Opponent Difficulty (Enum o Stringa)
    @Enumerated(EnumType.STRING)
    @Column(name = "opponent_difficulty", nullable = false)
    private OpponentDifficulty opponentDifficulty;

    @Column(name = "opponent_type")
    private String opponentType;

    @Column(name = "coverage")
    private String coverage;

    // 5. Tipi Complessi (Mappatura JSONB per flessibilità)
    // Richiede una libreria di terze parti (es. hibernate-types) o un custom Converter.
    @Type(type = "jsonb") // Annotazione per Hibernate Types
    @Column(name = "jacoco_score", columnDefinition = "jsonb")
    private JacocoScore jacocoScore;

    @Type(type = "jsonb")
    @Column(name = "evosuite_score", columnDefinition = "jsonb")
    private EvosuiteScore evosuiteScore;
}