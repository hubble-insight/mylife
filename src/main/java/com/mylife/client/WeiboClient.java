package com.mylife.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mylife.config.ApiConfig;
import com.mylife.model.WeiboPost;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Component
public class WeiboClient {

    private static final String BASE_URL = "https://api.weibo.com/2";
    private static final Gson GSON = new Gson();

    private final RestTemplate restTemplate;
    private final ApiConfig apiConfig;

    public WeiboClient(RestTemplate restTemplate, ApiConfig apiConfig) {
        this.restTemplate = restTemplate;
        this.apiConfig = apiConfig;
    }

    /**
     * Get user's recent weibo posts
     */
    public List<WeiboPost> getUserTimeline(int count) {
        String accessToken = apiConfig.getWeibo().getAccessToken();
        String userId = apiConfig.getWeibo().getUserId();

        if (accessToken == null || userId == null) {
            log.warn("Weibo API credentials not configured");
            return Collections.emptyList();
        }

        try {
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/statuses/user_timeline.json")
                .queryParam("access_token", accessToken)
                .queryParam("uid", userId)
                .queryParam("count", count)
                .toUriString();

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonObject json = GSON.fromJson(response.getBody(), JsonObject.class);
                return parseWeiboPosts(json.getAsJsonArray("statuses"));
            }
        } catch (Exception e) {
            log.error("Error fetching weibo timeline: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    private List<WeiboPost> parseWeiboPosts(com.google.gson.JsonArray statuses) {
        List<WeiboPost> posts = new ArrayList<>();

        for (int i = 0; i < statuses.size(); i++) {
            try {
                JsonObject status = statuses.get(i).getAsJsonObject();
                WeiboPost post = new WeiboPost();

                post.setWeiboId(status.get("idstr").getAsString());
                post.setContent(status.get("text").getAsString());

                // Parse created time (Wed Oct 10 20:22:58 +0800 2018)
                String createdAtStr = status.get("created_at").getAsString();
                post.setCreatedAt(parseWeiboDate(createdAtStr));

                post.setSource(status.has("source") ? status.get("source").getAsString() : null);

                // Get thumbnail or original image
                if (status.has("pic_urls") && status.getAsJsonArray("pic_urls").size() > 0) {
                    String thumbnailUrl = status.getAsJsonArray("pic_urls").get(0).getAsJsonObject()
                        .get("thumbnail_pic").getAsString();
                    post.setThumbnailUrl(thumbnailUrl);

                    String originalUrl = status.getAsJsonArray("pic_urls").get(0).getAsJsonObject()
                        .get("original_pic").getAsString();
                    post.setOriginalUrl(originalUrl);
                }

                post.setRepostsCount(status.has("reposts_count") ? status.get("reposts_count").getAsInt() : 0);
                post.setCommentsCount(status.has("comments_count") ? status.get("comments_count").getAsInt() : 0);
                post.setAttitudesCount(status.has("attitudes_count") ? status.get("attitudes_count").getAsInt() : 0);

                post.setSyncedAt(LocalDateTime.now());

                posts.add(post);
            } catch (Exception e) {
                log.warn("Error parsing weibo post: {}", e.getMessage());
            }
        }

        return posts;
    }

    /**
     * Parse Weibo date string format: "Wed Oct 10 20:22:58 +0800 2018"
     */
    private LocalDateTime parseWeiboDate(String dateStr) {
        try {
            // Weibo date format: EEE MMM dd HH:mm:ss Z yyyy
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                "EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);
            Instant instant = Instant.from(formatter.parse(dateStr));
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        } catch (Exception e) {
            log.warn("Failed to parse date: {}, using current time", dateStr);
            return LocalDateTime.now();
        }
    }
}
