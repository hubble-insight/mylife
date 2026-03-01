package com.mylife.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Weibo Post Entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "weibo_posts")
public class WeiboPost {

    @Id
    @Column(name = "weibo_id")
    private String weiboId;

    @Column(name = "content", length = 2000)
    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "source")
    private String source;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "original_url", length = 500)
    private String originalUrl;

    @Column(name = "reposts_count")
    private Integer repostsCount;

    @Column(name = "comments_count")
    private Integer commentsCount;

    @Column(name = "attitudes_count")
    private Integer attitudesCount;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;
}
