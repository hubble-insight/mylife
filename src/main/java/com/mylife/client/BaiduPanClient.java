package com.mylife.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mylife.model.CloudFile;
import com.mylife.model.FileType;
import com.mylife.service.OAuth2Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Component
public class BaiduPanClient {

    private static final String FILE_LIST_URL = "https://pan.baidu.com/rest/2.0/xpan/file";
    private static final String FILE_DOWNLOAD_URL = "https://d.pcs.baidu.com/rest/2.0/pcs/file";
    private static final Gson GSON = new Gson();

    private final OAuth2Service oauth2Service;
    private String rootDir = "/";

    public BaiduPanClient(OAuth2Service oauth2Service) {
        this.oauth2Service = oauth2Service;
    }

    /**
     * Set the root directory for Baidu Pan
     * @param rootDir Root directory path, e.g., "/apps/mylife"
     */
    public void setRootDir(String rootDir) {
        this.rootDir = rootDir != null && !rootDir.isEmpty() ? rootDir : "/";
    }

    /**
     * Get the current root directory
     */
    public String getRootDir() {
        return rootDir;
    }

    /**
     * Get file list from Baidu Pan
     * @param dirPath Directory path, e.g., "/apps/mylife"
     * @param start Start index
     * @param limit Number of files to fetch
     */
    public List<CloudFile> getFileList(String dirPath, int start, int limit) {
        String accessToken;
        try {
            accessToken = oauth2Service.getValidToken().getAccessToken();
            log.debug("获取到 access_token: {}...", accessToken.substring(0, Math.min(20, accessToken.length())));
        } catch (IllegalStateException e) {
            log.warn("未授权，请先进行 OAuth 授权：{}", e.getMessage());
            return Collections.emptyList();
        }

        BufferedReader in = null;
        try {
            // 路径包含中文时需要 URL 编码
            String encodedPath = URLEncoder.encode(dirPath, StandardCharsets.UTF_8.name());

            // 构建请求 URL
            String urlString = FILE_LIST_URL + "?method=list"
                + "&access_token=" + accessToken
                + "&dir=" + encodedPath
                + "&order=time"
                + "&start=" + start
                + "&limit=" + limit
                + "&web=web"
                + "&folder=0";

            log.info("请求百度网盘 API: {}", urlString);

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", "pan.baidu.com");

            in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }

            log.info("百度网盘 API 响应状态：{}", conn.getResponseCode());

            if (conn.getResponseCode() == 200 && response.length() > 0) {
                log.debug("API 响应内容：{}", response.toString());
                JsonObject json = GSON.fromJson(response.toString(), JsonObject.class);

                // 检查是否有错误
                if (json.has("error_code")) {
                    int errorCode = json.get("error_code").getAsInt();
                    String errorMsg = json.has("error_msg") ? json.get("error_msg").getAsString() : "Unknown";
                    log.error("百度网盘 API 返回错误：error_code={}, error_msg={}", errorCode, errorMsg);
                    return Collections.emptyList();
                }

                if (json.has("list") && json.get("list").isJsonArray()) {
                    com.google.gson.JsonArray list = json.getAsJsonArray("list");
                    log.info("获取到 {} 个文件", list.size());
                    return parseCloudFiles(list);
                } else {
                    log.warn("百度网盘 API 返回数据中没有 list 字段，完整响应：{}", response.toString());
                    return Collections.emptyList();
                }
            }
        } catch (Exception e) {
            log.error("获取百度网盘文件列表失败：{}", e.getMessage(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        return Collections.emptyList();
    }

    /**
     * Get file metadata
     */
    public Optional<CloudFile> getFileMetadata(String fileId) {
        String accessToken;
        try {
            accessToken = oauth2Service.getValidToken().getAccessToken();
        } catch (IllegalStateException e) {
            log.warn("未授权，请先进行 OAuth 授权");
            return Optional.empty();
        }

        if (accessToken == null) {
            return Optional.empty();
        }

        BufferedReader in = null;
        try {
            // 使用 filemanager 接口获取文件信息
            String urlString = FILE_LIST_URL + "?method=filemanager"
                + "&access_token=" + accessToken
                + "&func=list"
                + "&fid_list=[\"" + fileId + "\"]";

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", "pan.baidu.com");

            in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }

            if (conn.getResponseCode() == 200 && response.length() > 0) {
                JsonObject json = GSON.fromJson(response.toString(), JsonObject.class);
                com.google.gson.JsonArray list = json.getAsJsonArray("list");
                if (list != null && list.size() > 0) {
                    return Optional.of(parseCloudFile(list.get(0).getAsJsonObject()));
                }
            }
        } catch (Exception e) {
            log.error("Error fetching file metadata: {}", e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Get file download URL by making a request and getting the redirect URL.
     * @param fileId File ID
     * @return Download URL or null
     */
    public String getFileDownloadUrl(String fileId) {
        String accessToken;
        try {
            accessToken = oauth2Service.getValidToken().getAccessToken();
        } catch (IllegalStateException e) {
            log.warn("未授权，请先进行 OAuth 授权");
            return null;
        }

        if (accessToken == null) {
            return null;
        }

        HttpURLConnection conn = null;
        try {
            // First get file path
            Optional<CloudFile> fileInfo = getFileMetadata(fileId);
            if (fileInfo.isPresent()) {
                String path = fileInfo.get().getFilePath();
                // Build download API URL
                String apiUrl = FILE_DOWNLOAD_URL + "?method=download"
                    + "&access_token=" + accessToken
                    + "&path=" + URLEncoder.encode(path, StandardCharsets.UTF_8.name());
                log.info("Requesting download URL from: {}", apiUrl);

                URL url = new URL(apiUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                // The User-Agent MUST be 'pan.baidu.com'
                conn.setRequestProperty("User-Agent", "pan.baidu.com");
                // Disable auto-redirect to handle 302 response manually
                conn.setInstanceFollowRedirects(false);

                int responseCode = conn.getResponseCode();
                log.info("Baidu Pan download API response code: {}", responseCode);

                // Baidu Pan returns a 302 redirect to the actual download link
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                    String downloadUrl = conn.getHeaderField("Location");
                    log.info("Got redirect to actual download URL: {}", downloadUrl);
                    return downloadUrl;
                } else {
                    log.error("Failed to get download URL. Response code: {}, Response message: {}",
                              responseCode, conn.getResponseMessage());
                    // Log the response body for debugging
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        String line;
                        StringBuilder response = new StringBuilder();
                        while ((line = in.readLine()) != null) {
                            response.append(line);
                        }
                        log.error("Error response body: {}", response.toString());
                    } catch (Exception ex) {
                        // ignore if can't read body
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取文件下载链接失败：{}", e.getMessage(), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return null;
    }

    /**
     * Get file thumbnail URL (for images)
     * @param fileId File ID
     * @return Thumbnail URL or null
     */
    public String getFileThumbnailUrl(String fileId) {
        String accessToken;
        try {
            accessToken = oauth2Service.getValidToken().getAccessToken();
        } catch (IllegalStateException e) {
            log.warn("未授权，请先进行 OAuth 授权");
            return null;
        }

        if (accessToken == null) {
            return null;
        }

        try {
            Optional<CloudFile> fileInfo = getFileMetadata(fileId);
            if (fileInfo.isPresent()) {
                String path = fileInfo.get().getFilePath();
                // Build thumbnail URL (size: 1000x1000)
                String thumbnailUrl = FILE_LIST_URL + "?method=thumbnail"
                    + "&access_token=" + accessToken
                    + "&path=" + URLEncoder.encode(path, StandardCharsets.UTF_8.name())
                    + "&width=1000"
                    + "&height=1000";
                log.debug("File thumbnail URL: {}", thumbnailUrl);
                return thumbnailUrl;
            }
        } catch (Exception e) {
            log.error("获取文件缩略图失败：{}", e.getMessage());
        }

        return null;
    }

    private List<CloudFile> parseCloudFiles(com.google.gson.JsonArray list) {
        List<CloudFile> files = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            try {
                files.add(parseCloudFile(list.get(i).getAsJsonObject()));
            } catch (Exception e) {
                log.warn("Error parsing cloud file: {}", e.getMessage());
            }
        }

        return files;
    }

    private CloudFile parseCloudFile(JsonObject json) {
        CloudFile file = new CloudFile();

        file.setFileId(json.has("fs_id") ? String.valueOf(json.get("fs_id").getAsLong()) : null);
        file.setFileName(json.has("server_filename") ? json.get("server_filename").getAsString() :
                        (json.has("file_name") ? json.get("file_name").getAsString() : null));
        file.setFilePath(json.has("path") ? json.get("path").getAsString() : null);
        file.setFileSize(json.has("size") ? json.get("size").getAsLong() : 0L);
        file.setIsDir(json.has("isdir") ? json.get("isdir").getAsInt() == 1 :
                     (json.has("is_dir") ? json.get("is_dir").getAsBoolean() : false));

        // Determine file type from extension
        String fileName = file.getFileName();
        String extension = "";
        if (fileName != null && fileName.contains(".")) {
            extension = fileName.substring(fileName.lastIndexOf(".") + 1);
        }
        file.setExtension(extension);
        file.setFileType(file.getIsDir() ? FileType.OTHER : FileType.fromExtension(extension));

        // Parse timestamps
        if (json.has("server_ctime")) {
            file.setCreatedTime(LocalDateTime.ofInstant(
                Instant.ofEpochSecond(json.get("server_ctime").getAsLong()),
                ZoneId.systemDefault()));
        }
        if (json.has("server_mtime")) {
            file.setModifiedTime(LocalDateTime.ofInstant(
                Instant.ofEpochSecond(json.get("server_mtime").getAsLong()),
                ZoneId.systemDefault()));
        }

        file.setSyncedAt(LocalDateTime.now());

        return file;
    }
}
