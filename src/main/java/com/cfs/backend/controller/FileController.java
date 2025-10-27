package com.cfs.backend.controller;

import com.cfs.backend.entity.FileNode;
import com.cfs.backend.entity.User;
import com.cfs.backend.repo.FileNodeRepo;
import com.cfs.backend.repo.UserRepo;
import com.cfs.backend.security.SecurityUser;
import com.cfs.backend.services.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileNodeRepo fileNodeRepo;
    private final StorageService storageService;
    private final UserRepo userRepo;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @AuthenticationPrincipal SecurityUser securityUser,
            @RequestParam("file") MultipartFile file,
            @RequestParam("parentId") Long parentId
            ){
        if(securityUser == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not logged in");
        }
        if(file.isEmpty()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is empty");
        }

        try {
            User user = securityUser.getUser();
            FileNode parentFolder = fileNodeRepo.findById(parentId)
                    .orElseThrow(() -> new RuntimeException("Parent folder not found"));

            if (!parentFolder.getOwner().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized");
            }

            if (user.getStorageUsed() + file.getSize() > user.getStorageAlloted()) {
                   return ResponseEntity.status(HttpStatus.INSUFFICIENT_STORAGE).body("Not enough storage");
            }

            String storagePath = storageService.uploadFile(file, user.getId());
            FileNode newFile = new FileNode();
            newFile.setFileName(file.getOriginalFilename());
            newFile.setIsDirectory(false);
            newFile.setFileSize(file.getSize());
            newFile.setMimeType(file.getContentType());
            newFile.setStoragePath(storagePath);
            newFile.setOwner(user);
            newFile.setParent(parentFolder);
            newFile.setDeleted(false);
            newFile.setIsLocked(false);

            FileNode savedfile =  fileNodeRepo.save(newFile);
            user.setStorageUsed(user.getStorageUsed() + file.getSize());
            userRepo.save(user);
            return ResponseEntity.status(200).body("File uploaded successfully");


        }
        catch (Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }

    }

}
