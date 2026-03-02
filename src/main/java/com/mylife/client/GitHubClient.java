package com.mylife.client;

import com.mylife.config.ApiConfig;
import com.mylife.model.GitHubActivity;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class GitHubClient {

    private final ApiConfig apiConfig;

    public GitHubClient(ApiConfig apiConfig) {
        this.apiConfig = apiConfig;
    }

    public List<GitHubActivity> getPublicActivity() {
        String username = apiConfig.getGithub().getUsername();

        if (username == null || username.isEmpty()) {
            log.warn("GitHub username not configured");
            return Collections.emptyList();
        }

        String feedUrl = "https://github.com/" + username + ".atom";
        List<GitHubActivity> activities = new ArrayList<>();

        try (XmlReader reader = new XmlReader(new URL(feedUrl))) {
            SyndFeed feed = new SyndFeedInput().build(reader);

            for (SyndEntry entry : feed.getEntries()) {
                GitHubActivity activity = new GitHubActivity();
                activity.setEventId(entry.getUri());
                activity.setTitle(entry.getTitle());
                activity.setLink(entry.getLink());

                // Handle null published date
                if (entry.getPublishedDate() != null) {
                    activity.setPublishedAt(
                        LocalDateTime.ofInstant(entry.getPublishedDate().toInstant(), ZoneId.systemDefault()));
                } else if (entry.getUpdatedDate() != null) {
                    activity.setPublishedAt(
                        LocalDateTime.ofInstant(entry.getUpdatedDate().toInstant(), ZoneId.systemDefault()));
                } else {
                    activity.setPublishedAt(LocalDateTime.now());
                }

                activity.setSyncedAt(LocalDateTime.now());
                activities.add(activity);
            }
        } catch (Exception e) {
            log.error("Error fetching GitHub activity feed: {}", e.getMessage());
        }

        return activities;
    }
}
