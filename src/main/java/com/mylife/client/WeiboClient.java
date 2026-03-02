package com.mylife.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mylife.config.ApiConfig;
import com.mylife.model.WeiboPost;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class WeiboClient {

    private static final Gson GSON = new Gson();

    private final RestTemplate restTemplate;
    private final ApiConfig apiConfig;

    public WeiboClient(RestTemplate restTemplate, ApiConfig apiConfig) {
        this.restTemplate = restTemplate;
        this.apiConfig = apiConfig;
    }

    /**
     * Get user's recent weibo posts from RSSHub
     */
    public List<WeiboPost> getUserTimeline(int count) {
        String rssUrl = apiConfig.getWeibo().getRssUrl();

        if (rssUrl == null || rssUrl.isEmpty()) {
            log.warn("Weibo RSS URL not configured");
            return Collections.emptyList();
        }

        // Append .json to get JSON format
        if (!rssUrl.endsWith(".json")) {
            rssUrl += ".json";
        }

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(rssUrl, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonObject json = GSON.fromJson(response.getBody(), JsonObject.class);
                return parseRssItems(json.getAsJsonArray("items"));
            }
        } catch (Exception e) {
            log.error("Error fetching Weibo RSS feed: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    private List<WeiboPost> parseRssItems(com.google.gson.JsonArray items) {
        List<WeiboPost> posts = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            try {
                JsonObject item = items.get(i).getAsJsonObject();
                WeiboPost post = new WeiboPost();

                post.setWeiboId(item.get("id").getAsString());
                post.setContent(item.get("content_html").getAsString());

                // Parse date_published (ISO 8601 format)
                String createdAtStr = item.get("date_published").getAsString();
                post.setCreatedAt(parseIsoDate(createdAtStr));

                post.setSource(item.has("source") ? item.get("source").getAsJsonObject().get("name").getAsString() : "RSSHub");

                if (item.has("attachments") && item.getAsJsonArray("attachments").size() > 0) {
                    String imageUrl = item.getAsJsonArray("attachments").get(0).getAsJsonObject()
                        .get("url").getAsString();
                    post.setThumbnailUrl(imageUrl);
                    post.setOriginalUrl(imageUrl);
                }

                post.setSyncedAt(LocalDateTime.now());

                // RSS feed does not provide counts for reposts, comments, attitudes
                post.setRepostsCount(0);
                post.setCommentsCount(0);
                post.setAttitudesCount(0);

                posts.add(post);
            } catch (Exception e) {
                log.warn("Error parsing Weibo RSS item: {}", e.getMessage());
            }
        }

        return posts;
    }

    private LocalDateTime parseIsoDate(String dateStr) {
        try {
            return ZonedDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
        } catch (Exception e) {
            log.warn("Failed to parse date: {}, using current time", dateStr);
            return LocalDateTime.now();
        }
    }
}
