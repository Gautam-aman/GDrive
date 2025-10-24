package com.cfs.backend.repo;

import com.cfs.backend.entity.FileAccessLog;
import com.cfs.backend.entity.SharePermission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SharePermissionRepo extends JpaRepository<SharePermission, Long> {



}
