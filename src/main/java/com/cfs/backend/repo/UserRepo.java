package com.cfs.backend.repo;

import com.cfs.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.util.Optional;


public interface UserRepo extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email) ;
    Boolean existsByEmail(String email);
}
