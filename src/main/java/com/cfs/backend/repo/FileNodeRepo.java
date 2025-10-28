package com.cfs.backend.repo;

import com.cfs.backend.entity.FileNode;
import com.cfs.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.awt.print.Pageable;
import java.util.List;
import java.util.Optional;

public interface FileNodeRepo extends JpaRepository<FileNode, Long> {
    List<FileNode> findByParentAndOwnerAndIsDeletedFalse(FileNode parent, User owner);
    Optional<FileNode> findByParentAndIsDeletedFalse(FileNode fileNode);
    Boolean findByParentAndFileNameAndOwner(FileNode file ,  String name, User owner);
    Optional<FileNode> findBy(Long id, User owner);
    List<FileNode> findByOwnerAndIsDirectoryFalseAndIsDeletedFalse(User owner, Pageable pageable);
    Optional<FileNode> findByParentAndFileNameAndOwnerAndIsDeletedFalse(FileNode file, String name , User owner);

    List<FileNode> findByParentandOwner(FileNode file, User user);
}
