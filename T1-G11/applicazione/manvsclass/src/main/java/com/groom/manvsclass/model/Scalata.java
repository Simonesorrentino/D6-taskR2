package com.groom.manvsclass.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "scalate")
public class Scalata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "number_rounds", nullable = false)
    private Integer numberRounds;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "scalata_classes",
            joinColumns = @JoinColumn(name = "scalata_id"), // Lato Scalata
            inverseJoinColumns = @JoinColumn(name = "class_name") // Lato ClassUT
    )
    @ToString.Exclude
    private List<ClassUT> classes = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email", referencedColumnName = "email", nullable = false)
    @ToString.Exclude
    private Admin creator;

}
