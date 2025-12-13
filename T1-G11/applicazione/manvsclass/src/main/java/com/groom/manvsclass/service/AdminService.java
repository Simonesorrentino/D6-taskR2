package com.groom.manvsclass.service;

import com.groom.manvsclass.model.entity.AdminEntity;
import com.groom.manvsclass.model.ClassUT;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface AdminService {

    ResponseEntity<AdminEntity> getAdminByUsername(String username, String jwt);

    ResponseEntity<?> loginWithInvitation(AdminEntity adminEntity, String jwt);

    ResponseEntity<?> inviteAdmins(AdminEntity adminEntity, String jwt);

    ResponseEntity<List<ClassUT>> filtraClassi(String text, String category, String jwt);

    ResponseEntity<List<ClassUT>> filtraClassi(String category, String jwt);
}
