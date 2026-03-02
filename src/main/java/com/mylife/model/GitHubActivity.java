package com.mylife.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "github_activities")
public class GitHubActivity {

    @Id
    private String eventId;

    private String title;

    private String link;

    private LocalDateTime publishedAt;

    private LocalDateTime syncedAt;
}
