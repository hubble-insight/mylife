package com.mylife.model;

import lombok.Data;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
