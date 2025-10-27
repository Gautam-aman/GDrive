package com.cfs.backend.dto;


import lombok.Data;
import lombok.Getter;
import org.springframework.stereotype.Service;

@Data
@Getter
@Service
public class UserResponse {
    private Long id;
    private String email;
    private String storageQuota;
    private String storageUsed;
}
