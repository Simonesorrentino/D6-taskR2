package com.groom.manvsclass.model.repository;

import com.groom.manvsclass.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRepository extends JpaRepository<Admin, String> {

    Admin findByInvitationToken(String invitationToken);

    Admin findByUsername(String username);

    Admin findByEmail(String email);

    Admin findByResetToken(String resetToken);
}
