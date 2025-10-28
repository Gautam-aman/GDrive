package com.cfs.backend.controller;

import com.cfs.backend.dto.RenameRequest;
import com.cfs.backend.entity.FileNode;
import com.cfs.backend.entity.User;
import com.cfs.backend.repo.FileNodeRepo;
import com.cfs.backend.repo.UserRepo;
import com.cfs.backend.security.SecurityUser;
import com.cfs.backend.services.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.print.Pageable;
import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileNodeRepo fileNodeRepo;
    private final StorageService storageService;
    private final UserRepo userRepo;

    @PostMapping("/upload")
    @Transactional
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

    @GetMapping("/list")
    public ResponseEntity<?> listContent(
            @AuthenticationPrincipal SecurityUser securityUser,
            @RequestParam("parentId") Long parentId
    ){
        if(securityUser == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not logged In");
        }

        try{
            User user = securityUser.getUser();
            FileNode parentFolder = fileNodeRepo.findById(parentId)
                    .orElseThrow(() -> new RuntimeException("Folder not found"));
            if(!parentFolder.getOwner().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized");
            }
            List<FileNode> content = fileNodeRepo.findByParentAndOwnerAndIsDeletedFalse(parentFolder, user);
            return ResponseEntity.status(200).body(content);

        }
        catch (Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }

    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<?> downloadFile(@PathVariable Long fileId  , @AuthenticationPrincipal SecurityUser securityUser){
        if (securityUser == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not logged in");
        }
        try{
            FileNode file =  fileNodeRepo.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found"));

            if(!file.getOwner().getId().equals(securityUser.getUser().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized");
            }

            if(file.getIsDirectory()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("You can not download a Folder");
            }

            if (file.isDeleted()){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("This file is Deleted");
            }

            String downloadUrl = storageService.generateDownloadUrl(file.getStoragePath());

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(downloadUrl))
                    .build();


        }
        catch (Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    @GetMapping("/recent")
    public ResponseEntity<?> getRecentFiles(@AuthenticationPrincipal SecurityUser securityUser){
        if (securityUser == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not logged in");
        }
        try {
            User user = securityUser.getUser();
            Pageable pageable = (Pageable) PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));
            List<FileNode> recentFiles = fileNodeRepo.findByOwnerAndIsDirectoryFalseAndIsDeletedFalse(user, pageable);
            return ResponseEntity.ok(recentFiles);

        }
        catch (Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    @PatchMapping("/")
    public ResponseEntity<?> renameFile(@AuthenticationPrincipal SecurityUser securityUser, @RequestParam("fileId") Long fileId,
                                        RenameRequest renameRequest){

        if(securityUser == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not logged in");
        }

        User user = securityUser.getUser();
        FileNode file = fileNodeRepo.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (file.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized");
        }

        try{
            String newName = renameRequest.getNewName().trim();
            if (newName.contains("/") || newName.contains("\\")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Name cannot contain '/' or '\\'");
            }
            FileNode parentNode = file.getParent();
           Optional<FileNode> existingFile = fileNodeRepo.findByParentAndFileNameAndOwnerAndIsDeletedFalse(parentNode, newName, user);
           if (existingFile.isPresent()  && !existingFile.get().getId().equals(file.getId())){
               return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File already exists");
           }
           file.setFileName(newName);
            FileNode updatedNode = fileNodeRepo.save(file);
            return ResponseEntity.ok(updatedNode);
        }
        catch (Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }

    }

    @DeleteMapping("/{fileId}")
    @Transactional
    public ResponseEntity<?> deleteFile(@PathVariable Long fileId , @AuthenticationPrincipal SecurityUser securityUser){
        if (securityUser == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not logged in");
        }
        try{
            User user = securityUser.getUser();
            FileNode file = fileNodeRepo.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found"));

            if(file.getOwner().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized");
            }

            if(file.isDeleted()){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("This file is Deleted");
            }

            long totalSizeDeleted = softDelete(file , user);
            user.setStorageAlloted(user.getStorageUsed() - totalSizeDeleted);
            userRepo.save(user);
            return ResponseEntity.status(HttpStatus.OK).body("File Deleted Successfully");



        }
        catch (Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    private long softDelete(FileNode file, User user) {
        if(file.isDeleted()){
             return 0;
        }

    }

}
