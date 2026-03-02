package com.mylife.controller;

import com.mylife.model.WeiboPost;
import com.mylife.model.CloudFile;
import com.mylife.model.FileType;
import com.mylife.model.GitHubActivity;
import com.mylife.service.WeiboService;
import com.mylife.service.BaiduPanService;
import com.mylife.service.GitHubService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/home")
public class HomeController {

    private final WeiboService weiboService;
    private final BaiduPanService baiduPanService;
    private final GitHubService gitHubService;

    public HomeController(WeiboService weiboService, BaiduPanService baiduPanService, GitHubService gitHubService) {
        this.weiboService = weiboService;
        this.baiduPanService = baiduPanService;
        this.gitHubService = gitHubService;
    }

    @GetMapping
    public String home(Model model) {
        // Get statistics
        Map<String, Object> stats = new HashMap<>();
        stats.put("weiboCount", weiboService.getPostCount());
        stats.put("documentCount", baiduPanService.getFileCountByType().getOrDefault(FileType.DOCUMENT, 0L));
        stats.put("videoCount", baiduPanService.getFileCountByType().getOrDefault(FileType.VIDEO, 0L));
        stats.put("audioCount", baiduPanService.getFileCountByType().getOrDefault(FileType.AUDIO, 0L));
        stats.put("githubCount", gitHubService.getActivityCount());
        stats.put("totalFiles", baiduPanService.getTotalFileCount());
        model.addAttribute("stats", stats);

        return "home";
    }
}
