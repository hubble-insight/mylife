package com.mylife.service;

import com.mylife.client.GitHubClient;
import com.mylife.model.GitHubActivity;
import com.mylife.repository.GitHubActivityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class GitHubService {

    private final GitHubClient gitHubClient;
    private final GitHubActivityRepository gitHubActivityRepository;

    public GitHubService(GitHubClient gitHubClient, GitHubActivityRepository gitHubActivityRepository) {
        this.gitHubClient = gitHubClient;
        this.gitHubActivityRepository = gitHubActivityRepository;
    }

    @Cacheable(value = "github-activities", key = "'latest'")
    public List<GitHubActivity> getLatestActivities() {
        return gitHubActivityRepository.findTop20ByOrderByPublishedAtDesc();
    }

    @Transactional
    public List<GitHubActivity> syncActivities() {
        log.info("Syncing GitHub activities...");
        List<GitHubActivity> activities = gitHubClient.getPublicActivity();

        if (!activities.isEmpty()) {
            gitHubActivityRepository.saveAll(activities);
            log.info("Synced {} GitHub activities", activities.size());
        }

        return activities;
    }

    public long getActivityCount() {
        return gitHubActivityRepository.count();
    }

    @Transactional
    public List<GitHubActivity> syncAndGetLatest() {
        syncActivities();
        return getLatestActivities();
    }
}
