package com.groom.manvsclass.model.entity;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Entity(name = "class_ut")
public class ClassUTEntity {

    @Id
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "difficulty")
    private String difficulty;

    @Column(name = "uri")
    private String uri;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", columnDefinition = "JSONB")
    private String category; // Mappato come String per un semplice JSONB

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToMany(mappedBy = "selectedClasses", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<ScalataEntity> scalate;
}