package com.cfs.backend.repo;

import com.cfs.backend.entity.FileAccessLog;
import com.cfs.backend.entity.FileNode;
import com.cfs.backend.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


import java.awt.print.Pageable;
import java.util.List;
import java.util.Optional;

public interface FileAccessLogRepo extends JpaRepository<FileAccessLog, Long> {

    Optional<FileAccessLog> findByUserAndFileNode(User user, FileNode fileNode);


    // it is jpql which access user form fal(file access log) and sort it by lastaccessat
    @Query("SELECT fal.fileNode FROM FileAccessLog fal " +
            "WHERE fal.user = :user AND fal.fileNode.isDeleted = false " +
            "ORDER BY fal.LastAccessedAt DESC")
    List<FileNode> findRecentFilesForUser(User user, Pageable pageable);

}
