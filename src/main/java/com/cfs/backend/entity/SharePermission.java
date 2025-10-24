package com.cfs.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

enum PermissionType{
    VIEW ,
    EDIT
}

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "share_permissions" , uniqueConstraints = {
        @UniqueConstraint(columnNames = {"file_node_id" , "user_id"})
})
public class SharePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_node_id" , nullable = false)
    private FileNode fileNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id"  , nullable = false)
    private User sharedWithUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PermissionType permissionType;

}
