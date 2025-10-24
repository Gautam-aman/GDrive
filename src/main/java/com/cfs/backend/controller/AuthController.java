package com.cfs.backend.controller;

import com.cfs.backend.dto.SignIn;
import com.cfs.backend.dto.SignUpRequest;
import com.cfs.backend.entity.FileNode;
import com.cfs.backend.entity.User;
import com.cfs.backend.repo.FileNodeRepo;
import com.cfs.backend.repo.UserRepo;
import com.cfs.backend.security.SecurityUser;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Data
@RequestMapping("/auth")
public class AuthController {

    private UserRepo userRepo;
    private PasswordEncoder passwordEncoder;
    private FileNodeRepo fileNodeRepo;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody SignUpRequest signUpRequest) {
            if(userRepo.findByEmail(signUpRequest.getEmail()).isPresent()){
                return ResponseEntity.badRequest().body("Email already exists");
            }
            User user = new User();
            user.setEmail(signUpRequest.getEmail());
            user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
            User savedUser = userRepo.save(user);

            // Root folder for New User
            FileNode rootfolder = new FileNode();
            rootfolder.setFileName("rootfolder");
            rootfolder.setIsDirectory(true);
            rootfolder.setFileSize(0L);
            rootfolder.setOwner(savedUser);
            rootfolder.setParent(null);
            rootfolder.setIsLocked(false);
            rootfolder.setDeleted(false);
            FileNode savedFileNode = fileNodeRepo.save(rootfolder);
            savedUser.setRootFolder(savedFileNode);
            userRepo.save(savedUser);
            return ResponseEntity.ok("User registered successfully");

    }

    @PostMapping("/login")
    public ResponseEntity<?> getLoggedInUser(@AuthenticationPrincipal SecurityUser securityUser) {
        if(securityUser == null){
            return ResponseEntity.status(400).body("User not found");
        }
        return ResponseEntity.ok(securityUser.getUser());
    }

}
