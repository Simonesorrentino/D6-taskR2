package com.groom.manvsclass.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.groom.manvsclass.model.enums.HintTypeEnum;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.*;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Entity(name = "hints")
public class Hint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "admin_email", referencedColumnName = "email", nullable = false)
    private AdminEntity admin;

    @ManyToOne
    @JoinColumn(name = "class_ut_name", referencedColumnName = "name")
    private ClassUTEntity classUt;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Type(type = "jsonb")
    @Column(name = "code", columnDefinition = "JSONB")
    private HintCode code;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private HintTypeEnum type;

    @Column(name = "\"order\"", nullable = false)
    private Integer order;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @JsonProperty("classUTName")
    public void setClassUTName(String className) {
        if (this.classUt == null) {
            this.classUt = new ClassUTEntity();
        }
        this.classUt.setName(className);
    }

    @JsonProperty("classUTName")
    public String getClassUTName() {
        return this.classUt != null ? this.classUt.getName() : null;
    }
}