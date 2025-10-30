package com.cfs.backend.dto;

import com.cfs.backend.entity.PermissionType;
import lombok.Data;

@Data
public class ShareRequest {
    private String userName;
    PermissionType permissionType;
}
