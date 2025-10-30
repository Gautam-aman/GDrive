package com.cfs.backend.controller;

import com.cfs.backend.dto.AccessRequest;
import com.cfs.backend.dto.LockRequest;
import com.cfs.backend.dto.RenameRequest;
import com.cfs.backend.dto.ShareRequest;
import com.cfs.backend.entity.FileNode;
import com.cfs.backend.entity.PermissionType;
import com.cfs.backend.entity.SharePermission;
import com.cfs.backend.entity.User;
import com.cfs.backend.repo.FileNodeRepo;
import com.cfs.backend.repo.SharePermissionRepo;
import com.cfs.backend.repo.UserRepo;
import com.cfs.backend.security.SecurityUser;
import com.cfs.backend.services.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileNodeRepo fileNodeRepo;
    private final StorageService storageService;
    private final UserRepo userRepo;
    private final SharePermissionRepo sharePermissionRepo;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/upload")
    @Transactional
    public ResponseEntity<String> uploadFile(
            @AuthenticationPrincipal SecurityUser securityUser,
            @RequestParam("file") MultipartFile file,
            @RequestParam("parentId") Long parentId,
            @RequestParam(name = "password", required = false) String password) {

        if (securityUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not logged in");
        }
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is empty");
        }

        try {
            User user = securityUser.getUser();
            FileNode parentFolder = fileNodeRepo.findById(parentId)
                    .orElseThrow(() -> new RuntimeException("Parent folder not found"));

            if (!hasAccess(user, parentFolder, PermissionType.EDIT)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You do not have edit permissions for this folder");
            }

            FileNode lockedParent = findFirstLockedParent(parentFolder);
            if (lockedParent != null) {
                if (password == null || !passwordEncoder.matches(password, lockedParent.getFolderPassword())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Password required or invalid");
                }
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

            FileNode savedfile = fileNodeRepo.save(newFile);
            user.setStorageUsed(user.getStorageUsed() + file.getSize());
            userRepo.save(user);
            return ResponseEntity.status(200).body("File uploaded successfully");

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    @PostMapping("/list")
    public ResponseEntity<?> listContent(
            @AuthenticationPrincipal SecurityUser securityUser,
            @RequestParam("parentId") Long parentId,
            @RequestBody(required = false) AccessRequest request) {

        if (securityUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not logged In");
        }

        try {
            User user = securityUser.getUser();
            FileNode parentFolder = fileNodeRepo.findById(parentId)
                    .orElseThrow(() -> new RuntimeException("Folder not found"));

            if (!hasAccess(user, parentFolder, PermissionType.VIEW)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized");
            }

            if (Boolean.TRUE.equals(parentFolder.getIsLocked())) {
                String password = (request != null) ? request.getPassword() : null;
                if (password == null || !passwordEncoder.matches(password, parentFolder.getFolderPassword())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Password required or invalid");
                }
            }

            List<FileNode> content = fileNodeRepo.findByOwnerAndParent(user, parentFolder);
            return ResponseEntity.status(200).body(content);

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    @PostMapping("/download/{fileId}")
    public ResponseEntity<?> downloadFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal SecurityUser securityUser,
            @RequestBody(required = false) AccessRequest request) {

        if (securityUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not logged in");
        }
        try {
            User user = securityUser.getUser();
            FileNode file = fileNodeRepo.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found"));

            if (!hasAccess(user, file, PermissionType.VIEW)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized");
            }

            if (file.getIsDirectory()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("You can not download a Folder");
            }

            if (file.isDeleted()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("This file is Deleted");
            }

            FileNode lockedParent = findFirstLockedParent(file.getParent());
            if (lockedParent != null) {
                String password = (request != null) ? request.getPassword() : null;
                if (password == null || !passwordEncoder.matches(password, lockedParent.getFolderPassword())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Password required or invalid");
                }
            }

            String downloadUrl = storageService.generateDownloadUrl(file.getStoragePath());

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(downloadUrl))
                    .build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    @GetMapping("/recent")
    public ResponseEntity<?> getRecentFiles(@AuthenticationPrincipal SecurityUser securityUser) {
        if (securityUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not logged in");
        }
        try {
            User user = securityUser.getUser();
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));
            List<FileNode> recentFiles = fileNodeRepo.findByOwnerAndIsDirectoryFalseAndIsDeletedFalse(user, pageable);
            return ResponseEntity.ok(recentFiles);

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    @PatchMapping("/")
    public ResponseEntity<?> renameFile(
            @AuthenticationPrincipal SecurityUser securityUser,
            @RequestParam("fileId") Long fileId,
            @RequestBody RenameRequest renameRequest,
            @RequestParam(name = "password", required = false) String password) {

        if (securityUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not logged in");
        }

        User user = securityUser.getUser();
        FileNode file = fileNodeRepo.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (!hasAccess(user, file, PermissionType.EDIT)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You do not have edit permissions for this file");
        }

        FileNode lockedParent = findFirstLockedParent(file.getParent());
        if (lockedParent != null) {
            if (password == null || !passwordEncoder.matches(password, lockedParent.getFolderPassword())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Password required or invalid");
            }
        }

        try {
            String newName = renameRequest.getNewName().trim();
            if (newName.contains("/") || newName.contains("\\")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Name cannot contain '/' or '\\'");
            }
            FileNode parentNode = file.getParent();
            Optional<FileNode> existingFile = fileNodeRepo.findByParentAndFileNameAndOwnerAndIsDeletedFalse(parentNode, newName, user);
            if (existingFile.isPresent() && !existingFile.get().getId().equals(file.getId())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File already exists");
            }
            file.setFileName(newName);
            FileNode updatedNode = fileNodeRepo.save(file);
            return ResponseEntity.ok(updatedNode);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    @DeleteMapping("/{fileId}")
    @Transactional
    public ResponseEntity<?> deleteFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal SecurityUser securityUser,
            @RequestParam(name = "password", required = false) String password) {

        if (securityUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not logged in");
        }
        try {
            User user = securityUser.getUser();
            FileNode file = fileNodeRepo.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found"));

            if (!file.getOwner().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized");
            }

            if (file.isDeleted()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("This file is Deleted");
            }

            FileNode lockedParent = findFirstLockedParent(file.getParent());
            if (lockedParent != null) {
                if (password == null || !passwordEncoder.matches(password, lockedParent.getFolderPassword())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Password required or invalid");
                }
            }

            long totalSizeDeleted = softDelete(file, user);
            user.setStorageUsed(user.getStorageUsed() - totalSizeDeleted);
            userRepo.save(user);
            return ResponseEntity.status(HttpStatus.OK).body("File Deleted Successfully");

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    private long softDelete(FileNode file, User user) {
        if (file.isDeleted()) {
            return 0;
        }
        long totalSizeDeleted = 0;
        if (file.getIsDirectory()) {
            log.info("Deleting folder: " + file.getId());
            List<FileNode> folderToDelete = fileNodeRepo.findByParentAndOwnerAndIsDeletedFalse(file, user);
            for (FileNode folder : folderToDelete) {
                totalSizeDeleted += softDelete(folder, user);
            }
        } else {
            log.info("Deleting file: " + file.getId());
            totalSizeDeleted = file.getFileSize();
        }
        file.setDeleted(true);
        file.setDeletedAt(Instant.now());
        fileNodeRepo.save(file);
        return totalSizeDeleted;
    }

    @PatchMapping("/{fileId}/restore")
    public ResponseEntity<?> restoreFile(@PathVariable Long fileId, @AuthenticationPrincipal SecurityUser securityUser) {
        if (securityUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not logged in");
        }

        try {
            User user = securityUser.getUser();
            FileNode file = fileNodeRepo.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found"));
            if (!file.getOwner().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized");
            }
            if (!file.isDeleted()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is not Trashed");
            }

            FileNode parent = file.getParent();
            if (parent != null && parent.isDeleted()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Restore folder to Restore file");
            }
            long totalRestoreSize = getRestoreSize(user, file);
            long availableSize = user.getStorageAlloted() - user.getStorageUsed();
            if (totalRestoreSize > availableSize) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not enough storage . Required : " + (totalRestoreSize + availableSize));
            }
            privateRecursiveRestore(file, user);
            user.setStorageUsed(totalRestoreSize + user.getStorageUsed());
            userRepo.save(user);
            return ResponseEntity.status(HttpStatus.OK).body("File Restored Successfully");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    private void privateRecursiveRestore(FileNode file, User user) {
        if (!file.isDeleted()) {
            return;
        }
        file.setDeleted(false);
        file.setDeletedAt(null);
        fileNodeRepo.save(file);
        if (file.getIsDirectory()) {
            List<FileNode> folderToRestore = fileNodeRepo.findByParentAndOwner(file, user);
            for (FileNode child : folderToRestore) {
                if (child.isDeleted()) {
                    privateRecursiveRestore(child, user);
                }
            }
        }
    }

    private long getRestoreSize(User user, FileNode file) {
        if (!file.isDeleted()) {
            return 0;
        }
        long totalSize = 0;
        if (file.getIsDirectory()) {
            List<FileNode> folderToDelete = fileNodeRepo.findByParentAndOwner(file, user);
            for (FileNode folder : folderToDelete) {
                totalSize += getRestoreSize(user, folder);
            }
        } else {
            totalSize = file.getFileSize();
        }
        return totalSize;
    }

    @Transactional
    @PutMapping("/{fileId}/move")
    public ResponseEntity<?> moveFile(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long fileId,
            @RequestParam() Long targetFolderId,
            @RequestParam(name = "password", required = false) String password) {

        if (securityUser == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not logged in");
        }

        try {
            User user = securityUser.getUser();
            FileNode file = fileNodeRepo.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found"));

            if (!hasAccess(user, file, PermissionType.EDIT)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You do not have edit permissions for this file");
            }

            FileNode newParent = fileNodeRepo.findById(targetFolderId)
                    .orElseThrow(() -> new RuntimeException("Folder not found"));

            if (!hasAccess(user, newParent, PermissionType.EDIT)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You do not have edit permissions for this folder");
            }

            FileNode sourceLockedParent = findFirstLockedParent(file.getParent());
            FileNode targetLockedParent = findFirstLockedParent(newParent);

            if (sourceLockedParent != null) {
                if (password == null || !passwordEncoder.matches(password, sourceLockedParent.getFolderPassword())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Password required for source folder");
                }
            }
            if (targetLockedParent != null) {
                if (password == null || !passwordEncoder.matches(password, targetLockedParent.getFolderPassword())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Password required for target folder");
                }
            }

            if (!newParent.getIsDirectory()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("It is not folder");
            }
            if (isMovingToSameFolder(file, newParent)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Already in same folder");
            }

            Optional<FileNode> existingFile = fileNodeRepo.findByParentAndFileNameAndOwnerAndIsDeletedFalse(newParent, file.getFileName(), user);

            if (existingFile.isPresent()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("File already exists");
            }

            file.setParent(newParent);
            fileNodeRepo.save(file);

            return ResponseEntity.status(HttpStatus.OK).body("File Moved Successfully");

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    private boolean isMovingToSameFolder(FileNode file, FileNode newParent) {
        if (!file.getIsDirectory()) {
            return false;
        }
        FileNode currentParent = file.getParent();
        while (currentParent != null) {
            if (currentParent.getId().equals(newParent.getId())) {
                return true;
            }
            currentParent = currentParent.getParent();
        }
        return false;
    }

    @Transactional
    @DeleteMapping("/trash/{fileId}")
    public ResponseEntity<?> deleteFile(@AuthenticationPrincipal SecurityUser securityUser, @PathVariable Long fileId) {
        if (securityUser == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not logged in");
        }
        try {
            User user = securityUser.getUser();
            FileNode file = fileNodeRepo.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found"));
            if (!file.getOwner().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized");
            }
            if (!file.isDeleted()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("It is not Deleted");
            }
            privateRecursiveHardDelete(file, user);
            return ResponseEntity.status(HttpStatus.OK).body("File Deleted Successfully");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    private void privateRecursiveHardDelete(FileNode file, User user) {

        if (file.getIsDirectory()) {
            List<FileNode> child = fileNodeRepo.findByParentAndOwner(file, user);
            for (FileNode childFile : child) {
                privateRecursiveHardDelete(childFile, user);
            }
        }

        if (!file.getIsDirectory()) {
            try {
                storageService.deleteFile(file.getStoragePath());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        fileNodeRepo.delete(file);
    }

    @PostMapping("/{fileId}/share")
    @Transactional
    public ResponseEntity<?> shareFile(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long fileId,
            @RequestBody ShareRequest shareRequest) {

        if (securityUser == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not logged in");
        }
        User user = securityUser.getUser();
        FileNode file = fileNodeRepo.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (!file.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized");
        }

        User shareWith = userRepo.findByEmail(shareRequest.getUserName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getId().equals(shareWith.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can not share this file with yourself");
        }

        if (sharePermissionRepo.findByFileNodeAndSharedWithUser(file, shareWith).isPresent()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You Have already shared this file");
        }

        SharePermission sharePermission = new SharePermission();
        sharePermission.setFileNode(file);
        sharePermission.setSharedWithUser(shareWith);
        sharePermission.setPermissionType(shareRequest.getPermissionType());
        sharePermissionRepo.save(sharePermission);

        return ResponseEntity.status(HttpStatus.OK).body("Share Successfully");
    }

    @GetMapping("/shared-with-me")
    public ResponseEntity<?> getSharedWithMe(@AuthenticationPrincipal SecurityUser securityUser) {
        if (securityUser == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not logged in");
        }
        User user = securityUser.getUser();
        List<SharePermission> sharePermissions = sharePermissionRepo.findBySharedWithUser(user);
        List<FileNode> sharedFiles = sharePermissions.stream()
                .map(SharePermission::getFileNode)
                .filter(fileNode -> !fileNode.isDeleted())
                .toList();

        return ResponseEntity.status(HttpStatus.OK).body(sharedFiles);
    }

    private boolean hasAccess(User user, FileNode file, PermissionType requiredLevel) {
        FileNode current = file;
        while (current != null) {
            if (current.getOwner().getId().equals(user.getId())) {
                return true;
            }

            Optional<SharePermission> perm = sharePermissionRepo.findByFileNodeAndSharedWithUser(current, user);
            if (perm.isPresent()) {
                return hasSufficientPermission(perm.get().getPermissionType(), requiredLevel);
            }

            current = current.getParent();
        }
        return false;
    }

    private boolean hasSufficientPermission(PermissionType actual, PermissionType required) {
        if (actual == PermissionType.EDIT) {
            return true;
        }
        return actual == PermissionType.VIEW && required == PermissionType.VIEW;
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchFiles(
            @AuthenticationPrincipal SecurityUser securityUser,
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (securityUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not logged in");
        }

        User user = securityUser.getUser();

        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Search query cannot be empty");
        }

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<FileNode> results = fileNodeRepo.searchUserFiles(user, query.trim(), pageable);
            return ResponseEntity.ok(results);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    @PostMapping("/{folderId}/lock")
    @Transactional
    public ResponseEntity<?> lock(
            @PathVariable Long folderId,
            @RequestBody LockRequest lockRequest,
            @AuthenticationPrincipal SecurityUser securityUser) {

        if (securityUser == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not logged in");
        }
        User user = securityUser.getUser();
        FileNode file = fileNodeRepo.findById(folderId).orElseThrow(() -> new RuntimeException("File not found"));
        if (!file.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized");
        }
        if (!file.getIsDirectory()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("This is not a folder");
        }

        if (lockRequest.getPassword() == null || lockRequest.getPassword().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Password cannot be empty");
        }

        String hashPassword = passwordEncoder.encode(lockRequest.getPassword());
        file.setIsLocked(true);
        file.setFolderPassword(hashPassword);
        fileNodeRepo.save(file);

        return ResponseEntity.status(HttpStatus.OK).body("Folder Locked Successfully");
    }

    private FileNode findFirstLockedParent(FileNode file) {
        FileNode current = file;
        while (current != null) {
            if (Boolean.TRUE.equals(current.getIsLocked())) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }
}