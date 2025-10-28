package com.cfs.backend.dto;

import jakarta.annotation.Nullable;
import lombok.Data;
import software.amazon.awssdk.annotations.NotNull;

@Data
public class RenameRequest {

    @NotNull
    private String newName;

}
