package com.groom.manvsclass.model;

import lombok.Data;
import org.springframework.data.annotation.Id;

import javax.persistence.*;

@Data
@Entity
@Table(name = "achievements")
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // SERIAL in SQL
    private Long ID;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description",columnDefinition = "TEXT")
    private String description;

    @Column(name = "statistic")
    private String statistic;

    @Column(name = "progress_required")
    private float progressRequired;
}