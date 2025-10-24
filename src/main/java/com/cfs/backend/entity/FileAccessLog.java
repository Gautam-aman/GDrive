package com.cfs.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "file_access_log" , uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id" , "file_node_id"})
})
public class FileAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id" , nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="file_node_id" , nullable = false)
    private FileNode fileNode;

    @Column(nullable = false)
    private Instant LastAccessedAt;

}
