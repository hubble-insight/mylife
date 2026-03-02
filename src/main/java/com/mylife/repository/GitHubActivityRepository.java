package com.mylife.repository;

import com.mylife.model.GitHubActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GitHubActivityRepository extends JpaRepository<GitHubActivity, String> {

    List<GitHubActivity> findTop20ByOrderByPublishedAtDesc();
}
