package com.mylife.service;

import com.mylife.client.BaiduPanClient;
import com.mylife.config.ApiConfig;
import com.mylife.model.CloudFile;
import com.mylife.model.FileType;
import com.mylife.repository.CloudFileRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BaiduPanService {

    private String rootDir = "/";

    private final BaiduPanClient baiduPanClient;
    private final CloudFileRepository cloudFileRepository;
    private final ApiConfig apiConfig;

    public BaiduPanService(BaiduPanClient baiduPanClient, CloudFileRepository cloudFileRepository, ApiConfig apiConfig) {
        this.baiduPanClient = baiduPanClient;
        this.cloudFileRepository = cloudFileRepository;
        this.apiConfig = apiConfig;
    }

    @PostConstruct
    public void init() {
        // Get root directory from config
        this.rootDir = apiConfig.getBaiduPan().getRootDir();
        if (this.rootDir == null || this.rootDir.isEmpty()) {
            this.rootDir = "/";
        }
        // Set root directory in client
        baiduPanClient.setRootDir(this.rootDir);
        log.info("Baidu Pan root directory configured: {}", this.rootDir);
    }

    /**
     * Get latest files with caching
     */
    @Cacheable(value = "cloud-files", key = "'latest'")
    public List<CloudFile> getLatestFiles() {
        return cloudFileRepository.findTop10ByOrderByModifiedTimeDesc();
    }

    /**
     * Get files by type
     */
    @Cacheable(value = "cloud-files", key = "'type-' + #fileType.name")
    public List<CloudFile> getFilesByType(FileType fileType) {
        return cloudFileRepository.findByFileTypeOrderByModifiedTimeDesc(fileType);
    }

    /**
     * Sync files from Baidu Pan and save to database
     */
    @Transactional
    public List<CloudFile> syncFiles(int limit) {
        log.info("Syncing files from Baidu Pan (root: {})...", rootDir);
        List<CloudFile> files = baiduPanClient.getFileList(rootDir, 0, limit);

        // Filter out directories
        files = files.stream()
            .filter(f -> !f.getIsDir())
            .collect(Collectors.toList());

        if (!files.isEmpty()) {
            cloudFileRepository.saveAll(files);
            log.info("Synced {} files from Baidu Pan", files.size());
        }

        return files;
    }

    /**
     * Get file count by type
     */
    public Map<FileType, Long> getFileCountByType() {
        Map<FileType, Long> counts = new EnumMap<>(FileType.class);
        for (FileType type : FileType.values()) {
            counts.put(type, cloudFileRepository.countByFileType(type));
        }
        return counts;
    }

    /**
     * Get total file count
     */
    public long getTotalFileCount() {
        return cloudFileRepository.countByFileType(FileType.DOCUMENT) +
               cloudFileRepository.countByFileType(FileType.VIDEO) +
               cloudFileRepository.countByFileType(FileType.AUDIO) +
               cloudFileRepository.countByFileType(FileType.IMAGE) +
               cloudFileRepository.countByFileType(FileType.OTHER);
    }

    /**
     * Get document files
     */
    public List<CloudFile> getDocuments() {
        return getFilesByType(FileType.DOCUMENT);
    }

    /**
     * Get video files
     */
    public List<CloudFile> getVideos() {
        return getFilesByType(FileType.VIDEO);
    }

    /**
     * Get audio files
     */
    public List<CloudFile> getAudios() {
        return getFilesByType(FileType.AUDIO);
    }

    /**
     * Get image files
     */
    public List<CloudFile> getImages() {
        return getFilesByType(FileType.IMAGE);
    }

    /**
     * Get file by ID
     */
    public CloudFile getFileById(String fileId) {
        return cloudFileRepository.findById(fileId).orElse(null);
    }

    /**
     * Get file download URL
     */
    public String getFileDownloadUrl(String fileId) {
        CloudFile file = getFileById(fileId);
        if (file != null) {
            String downloadUrl = baiduPanClient.getFileDownloadUrl(fileId);
            if (downloadUrl != null) {
                file.setDownloadUrl(downloadUrl);
                cloudFileRepository.save(file);
            }
            return downloadUrl;
        }
        return null;
    }

    /**
     * Get file thumbnail URL
     */
    public String getFileThumbnailUrl(String fileId) {
        CloudFile file = getFileById(fileId);
        if (file != null) {
            String thumbnailUrl = baiduPanClient.getFileThumbnailUrl(fileId);
            if (thumbnailUrl != null) {
                file.setThumbnailUrl(thumbnailUrl);
                cloudFileRepository.save(file);
            }
            return thumbnailUrl;
        }
        return null;
    }

    /**
     * Sync and return latest files
     */
    @Transactional
    public List<CloudFile> syncAndGetLatest(int limit) {
        syncFiles(limit);
        return getLatestFiles();
    }
}
