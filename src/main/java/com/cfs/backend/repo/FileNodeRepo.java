package com.cfs.backend.repo;

import com.cfs.backend.entity.FileNode;
import com.cfs.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileNodeRepo extends JpaRepository<FileNode, Long> {
    List<FileNode> findByParentAndOwnerAndIsDeletedFalse(FileNode parent, User owner);
    Optional<FileNode> findByParentAndIsDeletedFalse(FileNode fileNode);
    Boolean findByParentAndFileNameAndOwner(FileNode file ,  String name, User owner);

    FileNode findByIdAndOwner(Long id, User owner);
    List<FileNode> findByOwnerAndIsDirectoryFalseAndIsDeletedFalse(User owner, Pageable pageable);
    Optional<FileNode> findByParentAndFileNameAndOwnerAndIsDeletedFalse(FileNode file, String name , User owner);

    List<FileNode> findByOwnerAndParent(User user,FileNode file);

    List<FileNode> findByParentAndOwner(FileNode parent, User owner);

    @Query("SELECT DISTINCT f FROM file_nodes f LEFT JOIN f.permissions p " +
            "WHERE f.isDeleted = false " +
            "AND LOWER(f.fileName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "AND (f.owner = :user OR p.sharedWithUser = :user)")
    Page<FileNode> searchUserFiles(
            @Param("user") User user,
            @Param("query") String query,
            Pageable pageable
    );

}
