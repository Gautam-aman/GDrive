package com.cfs.backend.repo;

import com.cfs.backend.entity.FileAccessLog;
import com.cfs.backend.entity.FileNode;
import com.cfs.backend.entity.SharePermission;
import com.cfs.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SharePermissionRepo extends JpaRepository<SharePermission, Long> {

    List<SharePermission> findBySharedWithUser(User user);

    Optional<SharePermission> findByFileNodeAndSharedWithUser(FileNode fileNode, User sharedWithUser);

}
