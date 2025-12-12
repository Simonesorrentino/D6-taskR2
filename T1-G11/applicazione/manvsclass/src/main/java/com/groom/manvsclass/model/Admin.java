package com.groom.manvsclass.model;

import com.groom.manvsclass.model.dto.Hint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "admins")
public class Admin {

    @Id
    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "surname", nullable = false)
    private String surname;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "invitation_token")
    private String invitationToken;

    @OneToMany(mappedBy = "creator") // "creator" è il nome del campo in Scalata.java
    @ToString.Exclude // FONDAMENTALE per evitare crash (StackOverflowError)
    private List<Scalata> scalate;

    @OneToMany(mappedBy = "admin") // "admin" è il nome del campo in Hint.java
    @ToString.Exclude
    private List<Hint> hints;

    @OneToMany(mappedBy = "user") // "user" è il nome del campo in Interaction.java
    @ToString.Exclude
    private List<Interaction> interactions;
}
