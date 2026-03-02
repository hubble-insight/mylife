package com.mylife.controller;

import com.mylife.model.GitHubActivity;
import com.mylife.service.GitHubService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/github")
public class GitHubController {

    private final GitHubService gitHubService;

    public GitHubController(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    @GetMapping
    public String getGitHubActivities(@RequestParam(required = false) boolean sync, Model model) {
        List<GitHubActivity> activities;

        if (sync) {
            activities = gitHubService.syncAndGetLatest();
        } else {
            activities = gitHubService.getLatestActivities();
        }

        model.addAttribute("activities", activities);
        return "github"; // Returns the github.html template
    }

    @GetMapping("/sync")
    public String syncGitHubActivities() {
        gitHubService.syncActivities();
        return "redirect:/github";
    }
}
