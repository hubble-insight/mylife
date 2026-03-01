package com.mylife.controller;

import com.mylife.model.CloudFile;
import com.mylife.model.FileType;
import com.mylife.service.BaiduPanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.URL;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/resources")
public class ResourceController {

    private final BaiduPanService baiduPanService;

    public ResourceController(BaiduPanService baiduPanService) {
        this.baiduPanService = baiduPanService;
    }

    @GetMapping
    public String resources(Model model,
                           @RequestParam(value = "type", required = false, defaultValue = "all") String type,
                           @RequestParam(value = "sync", required = false, defaultValue = "false") boolean sync) {

        if (sync) {
            baiduPanService.syncFiles(50);
        }

        // Get file counts by type
        Map<FileType, Long> counts = baiduPanService.getFileCountByType();
        model.addAttribute("fileCounts", counts);

        List<CloudFile> files;
        String currentType = type;

        if ("all".equals(type)) {
            files = baiduPanService.getLatestFiles();
            currentType = "all";
        } else if ("document".equals(type)) {
            files = baiduPanService.getDocuments();
            currentType = "document";
        } else if ("video".equals(type)) {
            files = baiduPanService.getVideos();
            currentType = "video";
        } else if ("audio".equals(type)) {
            files = baiduPanService.getAudios();
            currentType = "audio";
        } else if ("image".equals(type)) {
            files = baiduPanService.getImages();
            currentType = "image";
        } else {
            files = baiduPanService.getLatestFiles();
            currentType = "all";
        }

        model.addAttribute("files", files);
        model.addAttribute("currentType", currentType);

        return "resources";
    }

    @GetMapping("/sync")
    public String sync() {
        baiduPanService.syncFiles(50);
        return "redirect:/resources";
    }

    @GetMapping("/api/file")
    @ResponseBody
    public CloudFile getFile(@RequestParam String fileId) {
        return baiduPanService.getFileById(fileId);
    }

    @GetMapping("/api/file/{fileId}/download")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable String fileId) {
        try {
            CloudFile file = baiduPanService.getFileById(fileId);
            if (file != null) {
                String downloadUrl = baiduPanService.getFileDownloadUrl(fileId);
                if (downloadUrl != null) {
                    URL url = new URL(downloadUrl);
                    InputStreamResource resource = new InputStreamResource(url.openStream());

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentDispositionFormData("attachment", file.getFileName());
                    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

                    return ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(file.getFileSize())
                        .body(resource);
                }
            }
        } catch (Exception e) {
            log.error("下载文件失败：{}", e.getMessage());
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/api/file/{fileId}/view")
    public ResponseEntity<InputStreamResource> viewFile(@PathVariable String fileId) {
        try {
            CloudFile file = baiduPanService.getFileById(fileId);
            if (file != null) {
                String downloadUrl = baiduPanService.getFileDownloadUrl(fileId);
                if (downloadUrl != null) {
                    URL url = new URL(downloadUrl);
                    InputStreamResource resource = new InputStreamResource(url.openStream());

                    HttpHeaders headers = new HttpHeaders();
                    headers.setCacheControl("no-cache");

                    MediaType mediaType = getMediaType(file.getExtension());
                    headers.setContentType(mediaType);

                    return ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(file.getFileSize())
                        .body(resource);
                }
            }
        } catch (Exception e) {
            log.error("查看文件失败：{}", e.getMessage());
        }
        return ResponseEntity.notFound().build();
    }

    private MediaType getMediaType(String extension) {
        if (extension == null) return MediaType.APPLICATION_OCTET_STREAM;
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "webp" -> MediaType.valueOf("image/webp");
            case "mp4" -> MediaType.valueOf("video/mp4");
            case "webm" -> MediaType.valueOf("video/webm");
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "txt", "md" -> MediaType.TEXT_PLAIN;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
