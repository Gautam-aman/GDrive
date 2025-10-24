package com.cfs.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.catalina.AccessLog;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity(name = "file_nodes")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class FileNode {


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private Boolean isDirectory;

    private String mimeType; // type of file
    private Long fileSize; // size of fie in bytes

    private String storagePath;

    @Column(nullable = false)
    private boolean isDeleted = false;
    private Instant deletedAt;

    // This is for Locked folder
    private Boolean isLocked;
    private String folderPassword; // Will store hashed password

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id" ,  nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id" ,   nullable = false)
    private FileNode parent; // this will be null for root folder

    @OneToMany(mappedBy = "parent" ,  cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileNode> children;

    @OneToMany(mappedBy = "fileNode"  , cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SharePermission> permissions;

    @OneToMany(mappedBy = "fileNode"  ,  cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileAccessLog> accessLogs;

}
