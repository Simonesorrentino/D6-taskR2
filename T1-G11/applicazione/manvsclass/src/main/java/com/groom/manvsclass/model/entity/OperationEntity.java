package com.groom.manvsclass.model.entity;

import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity(name = "operation")
public class OperationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private int id_op;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email", nullable = false)
    private AdminEntity admin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "name", nullable = false)
    private ClassUTEntity classUt;

    private int type;
    private String date;
}