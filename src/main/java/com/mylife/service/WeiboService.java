package com.mylife.service;

import com.mylife.client.WeiboClient;
import com.mylife.model.WeiboPost;
import com.mylife.repository.WeiboPostRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class WeiboService {

    private final WeiboClient weiboClient;
    private final WeiboPostRepository weiboPostRepository;

    public WeiboService(WeiboClient weiboClient, WeiboPostRepository weiboPostRepository) {
        this.weiboClient = weiboClient;
        this.weiboPostRepository = weiboPostRepository;
    }

    /**
     * Get latest weibo posts with caching
     */
    @Cacheable(value = "weibo-posts", key = "'latest'")
    public List<WeiboPost> getLatestPosts() {
        return weiboPostRepository.findTop20ByOrderByCreatedAtDesc();
    }

    /**
     * Sync weibo posts from API and save to database
     */
    @Transactional
    public List<WeiboPost> syncPosts(int count) {
        log.info("Syncing weibo posts...");
        List<WeiboPost> posts = weiboClient.getUserTimeline(count);

        if (!posts.isEmpty()) {
            weiboPostRepository.saveAll(posts);
            log.info("Synced {} weibo posts", posts.size());
        }

        return posts;
    }

    /**
     * Get weibo post count
     */
    public long getPostCount() {
        return weiboPostRepository.count();
    }

    /**
     * Sync and return latest posts
     */
    @Transactional
    public List<WeiboPost> syncAndGetLatest(int count) {
        syncPosts(count);
        // Clear cache and get fresh data
        return getLatestPosts();
    }
}
