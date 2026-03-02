package com.mylife.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mylife.config.ApiConfig;
import com.mylife.model.WeiboPost;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
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

    // RSSHub mirrors (fallback options)
    private static final String[] RSSHUB_MIRRORS = {
        "https://rsshub.app",
        "https://rsshub.rssforever.com",
        "https://rsshub.zeabur.app"
    };

    // Alternative RSS sources
    private static final String[] ALTERNATIVE_SOURCES = {
        "https://feed.miantiao.me/weibo/rss/%s"  // 米条微博 RSS
    };

    private final RestTemplate restTemplate;
    private final ApiConfig apiConfig;

    public WeiboClient(RestTemplate restTemplate, ApiConfig apiConfig) {
        this.restTemplate = restTemplate;
        this.apiConfig = apiConfig;
    }

    /**
     * Get user's recent weibo posts from RSSHub or alternative sources
     */
    public List<WeiboPost> getUserTimeline(int count) {
        String rssUrl = apiConfig.getWeibo().getRssUrl();

        if (rssUrl == null || rssUrl.isEmpty()) {
            log.warn("Weibo RSS URL not configured");
            return Collections.emptyList();
        }

        // Try the configured URL first
        List<WeiboPost> posts = fetchFromUrl(rssUrl);
        if (!posts.isEmpty()) {
            return posts;
        }

        // Try RSSHub mirrors
        String weiboUid = extractWeiboUid(rssUrl);
        if (weiboUid != null) {
            for (String mirror : RSSHUB_MIRRORS) {
                String mirrorUrl = mirror + "/weibo/user/" + weiboUid;
                posts = fetchFromUrl(mirrorUrl);
                if (!posts.isEmpty()) {
                    log.info("Successfully fetched from mirror: {}", mirror);
                    return posts;
                }
            }
        }

        // Try alternative RSS sources
        if (weiboUid != null) {
            for (String source : ALTERNATIVE_SOURCES) {
                String altUrl = String.format(source, weiboUid);
                posts = fetchFromUrl(altUrl);
                if (!posts.isEmpty()) {
                    log.info("Successfully fetched from alternative source: {}", altUrl);
                    return posts;
                }
            }
        }

        log.warn("All Weibo RSS sources failed, returning empty list");
        return Collections.emptyList();
    }

    private List<WeiboPost> fetchFromUrl(String rssUrl) {
        // Append .json for RSSHub JSON format
        String url = rssUrl.endsWith(".json") ? rssUrl : rssUrl + ".json";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.set("Accept", "application/json, application/rss+xml, */*");

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                org.springframework.http.HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(headers),
                String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String body = response.getBody();

                // Try RSSHub JSON format
                if (body.contains("\"items\"")) {
                    JsonObject json = GSON.fromJson(body, JsonObject.class);
                    if (json.has("items") && json.get("items").isJsonArray()) {
                        return parseRssItems(json.getAsJsonArray("items"));
                    }
                }

                // Try standard RSS XML format
                if (body.contains("<rss") || body.contains("<feed")) {
                    return parseRssXml(body);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to fetch from {}: {}", rssUrl, e.getMessage());
        }
        return Collections.emptyList();
    }

    private List<WeiboPost> parseRssXml(String xml) {
        List<WeiboPost> posts = new ArrayList<>();
        try {
            // Simple RSS parsing - extract items from XML
            String[] items = xml.split("<item>");
            for (int i = 1; i < items.length && i <= 10; i++) {
                String item = items[i];

                WeiboPost post = new WeiboPost();

                // Extract link for weiboId
                String link = extractXmlTag(item, "link");
                String weiboId = generateWeiboIdFromLink(link, i);
                post.setWeiboId(weiboId);
                post.setOriginalUrl(link != null ? link : "");

                // Extract title
                String title = extractXmlTag(item, "title");
                post.setContent(title != null ? title : "");

                // Extract pubDate
                String pubDate = extractXmlTag(item, "pubDate");
                if (pubDate != null) {
                    post.setCreatedAt(parseDate(pubDate));
                } else {
                    post.setCreatedAt(LocalDateTime.now());
                }

                // Extract description/content if available
                String description = extractXmlTag(item, "description");
                if (description != null && !description.isEmpty()) {
                    // Try to extract image from description
                    if (description.contains("<img")) {
                        int imgSrc = description.indexOf("src=\"");
                        if (imgSrc != -1) {
                            imgSrc += 5;
                            int imgEnd = description.indexOf("\"", imgSrc);
                            if (imgEnd != -1) {
                                post.setThumbnailUrl(description.substring(imgSrc, imgEnd));
                            }
                        }
                    }
                }

                post.setSource("Weibo RSS");
                post.setSyncedAt(LocalDateTime.now());
                post.setRepostsCount(0);
                post.setCommentsCount(0);
                post.setAttitudesCount(0);

                posts.add(post);
            }
        } catch (Exception e) {
            log.warn("Error parsing RSS XML: {}", e.getMessage());
        }
        return posts;
    }

    private String generateWeiboIdFromLink(String link, int index) {
        if (link != null && !link.isEmpty()) {
            // Try to extract weibo ID from URL like https://weibo.com/6507972437/AbC123xyz
            String[] parts = link.split("/");
            for (int j = parts.length - 1; j >= 0; j--) {
                if (!parts[j].isEmpty() && !parts[j].contains("weibo.com")) {
                    return "wb_" + parts[j];
                }
            }
        }
        // Fallback to timestamp-based ID
        return "rss_" + System.currentTimeMillis() + "_" + index;
    }

    private String extractXmlTag(String xml, String tag) {
        String start = "<" + tag + ">";
        String end = "</" + tag + ">";
        int startIndex = xml.indexOf(start);
        int endIndex = xml.indexOf(end);
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return xml.substring(startIndex + start.length(), endIndex);
        }

        // Try CDATA format
        String cdataStart = "<" + tag + "><!\\[CDATA\\[";
        String cdataEnd = "\\]\\]></" + tag + ">";
        startIndex = xml.indexOf(cdataStart);
        endIndex = xml.indexOf(cdataEnd);
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return xml.substring(startIndex + cdataStart.length(), endIndex);
        }

        // Try self-closing with CDATA
        String cd = "<" + tag + "><![CDATA[";
        int cdStart = xml.indexOf(cd);
        if (cdStart != -1) {
            int cdEnd = xml.indexOf("]]>", cdStart);
            if (cdEnd != -1) {
                return xml.substring(cdStart + cd.length(), cdEnd);
            }
        }

        return null;
    }

    private List<WeiboPost> parseRssItems(com.google.gson.JsonArray items) {
        List<WeiboPost> posts = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            try {
                JsonObject item = items.get(i).getAsJsonObject();
                WeiboPost post = new WeiboPost();

                // Set weiboId - use id if available, otherwise generate from link or timestamp
                String weiboId = null;
                if (item.has("id") && item.get("id") != null) {
                    weiboId = item.get("id").getAsString();
                } else if (item.has("link") && item.get("link") != null) {
                    // Extract ID from link URL
                    String link = item.get("link").getAsString();
                    String[] parts = link.split("/");
                    for (int j = parts.length - 1; j >= 0; j--) {
                        if (!parts[j].isEmpty()) {
                            weiboId = "wb_" + parts[j];
                            break;
                        }
                    }
                }
                if (weiboId == null) {
                    weiboId = "wb_" + System.currentTimeMillis() + "_" + i;
                }
                post.setWeiboId(weiboId);

                post.setContent(item.get("content_html") != null ?
                    item.get("content_html").getAsString() :
                    (item.get("content") != null ? item.get("content").getAsString() : ""));

                // Parse date_published (ISO 8601 format)
                if (item.has("date_published")) {
                    String createdAtStr = item.get("date_published").getAsString();
                    post.setCreatedAt(parseIsoDate(createdAtStr));
                } else {
                    post.setCreatedAt(LocalDateTime.now());
                }

                post.setSource(item.has("source") ? item.get("source").getAsJsonObject().get("name").getAsString() : "RSSHub");

                if (item.has("attachments") && item.getAsJsonArray("attachments").size() > 0) {
                    String imageUrl = item.getAsJsonArray("attachments").get(0).getAsJsonObject()
                        .get("url").getAsString();
                    post.setThumbnailUrl(imageUrl);
                    post.setOriginalUrl(imageUrl);
                }

                post.setSyncedAt(LocalDateTime.now());
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

    private String extractWeiboUid(String rssUrl) {
        try {
            // Extract UID from URL like https://rsshub.app/weibo/user/6507972437
            String[] parts = rssUrl.split("/");
            for (int i = 0; i < parts.length; i++) {
                if ("user".equals(parts[i]) && i + 1 < parts.length) {
                    return parts[i + 1].replace(".json", "");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract Weibo UID: {}", e.getMessage());
        }
        return null;
    }

    private LocalDateTime parseIsoDate(String dateStr) {
        try {
            return ZonedDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
        } catch (Exception e) {
            log.warn("Failed to parse ISO date: {}, using current time", dateStr);
            return LocalDateTime.now();
        }
    }

    private LocalDateTime parseDate(String dateStr) {
        try {
            // Try RFC 822 format with GMT (common in RSS)
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH);
            try {
                return LocalDateTime.parse(dateStr, formatter);
            } catch (Exception e) {
                // Try RFC 822 format with timezone offset
                formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
                return ZonedDateTime.parse(dateStr, formatter).toLocalDateTime();
            }
        } catch (Exception e1) {
            try {
                // Try ISO format
                return ZonedDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
            } catch (Exception e2) {
                log.warn("Failed to parse date: {}, using current time", dateStr);
                return LocalDateTime.now();
            }
        }
    }
}
