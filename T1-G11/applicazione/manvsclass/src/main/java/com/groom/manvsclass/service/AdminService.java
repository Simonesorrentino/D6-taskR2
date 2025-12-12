package com.groom.manvsclass.service;

import com.groom.manvsclass.model.Admin;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface AdminService {

    ResponseEntity<Admin> getAdminByUsername(String username, String jwt);

    ResponseEntity<?> loginWithInvitation(Admin admin, String jwt);

    ResponseEntity<?> inviteAdmins(Admin admin, String jwt);

    ResponseEntity<List<ClassUT>> filtraClassi(String text, String category, String jwt);

    ResponseEntity<List<ClassUT>> filtraClassi(String category, String jwt);
}
