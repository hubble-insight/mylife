package com.mylife.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Cloud File Entity from Baidu Pan
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cloud_files")
public class CloudFile {

    @Id
    @Column(name = "file_id")
    private String fileId;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_type")
    @Enumerated(EnumType.STRING)
    private FileType fileType;

    @Column(name = "extension", length = 50)
    private String extension;

    @Column(name = "created_time")
    private LocalDateTime createdTime;

    @Column(name = "modified_time")
    private LocalDateTime modifiedTime;

    @Column(name = "download_url", length = 1000)
    private String downloadUrl;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(name = "is_dir")
    private Boolean isDir;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;
}
