package com.proximashare.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStats {
    private Integer totalFilesUploaded;
    private Long totalStorageUsed; // in bytes
    private Integer totalDownloads; // total download count across all files
}