package com.cfs.backend.entity;

import jakarta.persistence.*;
import jdk.jshell.spi.ExecutionControl;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(name ="users")
public class User  implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String password;

    @Column(unique = true , nullable = false)
    private String email;

    @Column(nullable = false)
    private Long storageAlloted = 5_000_000_000L; // 5Gb Default

    @Column(nullable = false)
    private Long storageUsed = 0L ; //Initial used storage is 0

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "root_folder_id")
    private FileNode rootFolder;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileNode> files;

    @OneToMany(mappedBy = "user"  , cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileAccessLog> logs;

    @OneToMany(mappedBy = "sharedWithUsers" , cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SharePermission> sharePermissions;

    // User Details Method

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        //Roles of user will be implemented later
        return List.of();
    }

    @Override
    public String getUsername() {
        return "";
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }






}
