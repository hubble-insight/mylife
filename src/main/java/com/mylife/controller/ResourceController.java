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

        Map<FileType, Long> counts = baiduPanService.getFileCountByType();
        model.addAttribute("fileCounts", counts);

        List<CloudFile> files;
        if ("all".equals(type)) {
            files = baiduPanService.getLatestFiles();
        } else if ("document".equals(type)) {
            files = baiduPanService.getDocuments();
        } else if ("video".equals(type)) {
            files = baiduPanService.getVideos();
        } else if ("audio".equals(type)) {
            files = baiduPanService.getAudios();
        } else if ("image".equals(type)) {
            files = baiduPanService.getImages();
        } else {
            files = baiduPanService.getLatestFiles();
        }

        model.addAttribute("files", files);
        model.addAttribute("currentType", type);

        return "resources";
    }

    @GetMapping("/api/file/{fileId}/direct-url")
    @ResponseBody
    public Map<String, String> getFileDirectUrl(@PathVariable String fileId) {
        String downloadUrl = baiduPanService.getFileDownloadUrl(fileId);
        return Map.of("url", downloadUrl);
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
                    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"");
                    headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
                    headers.add(HttpHeaders.PRAGMA, "no-cache");
                    headers.add(HttpHeaders.EXPIRES, "0");

                    return ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(file.getFileSize())
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
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
            case "ogg" -> MediaType.valueOf("video/ogg");
            case "mp3" -> MediaType.valueOf("audio/mpeg");
            case "wav" -> MediaType.valueOf("audio/wav");
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "txt", "md", "log", "csv", "java", "js", "py", "go", "rs", "c", "cpp", "h", "html", "css", "json", "xml", "yaml" -> MediaType.TEXT_PLAIN;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
