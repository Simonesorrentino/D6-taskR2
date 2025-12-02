package com.groom.manvsclass.model.repository;

import com.groom.manvsclass.model.AdminEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRepository extends JpaRepository<AdminEntity, String> {

    AdminEntity findByInvitationToken(String invitationToken);
}
