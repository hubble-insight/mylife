package com.mylife.controller;

import com.mylife.model.WeiboPost;
import com.mylife.service.WeiboService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/weibo")
public class WeiboController {

    private final WeiboService weiboService;

    public WeiboController(WeiboService weiboService) {
        this.weiboService = weiboService;
    }

    @GetMapping
    public String weiboList(Model model,
                           @RequestParam(value = "sync", required = false, defaultValue = "false") boolean sync) {
        List<WeiboPost> posts;

        if (sync) {
            posts = weiboService.syncAndGetLatest(20);
        } else {
            posts = weiboService.getLatestPosts();
        }

        model.addAttribute("posts", posts);
        return "weibo";
    }

    @GetMapping("/sync")
    public String sync() {
        weiboService.syncPosts(20);
        return "redirect:/weibo";
    }
}
