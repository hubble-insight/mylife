package com.mylife.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "api")
public class ApiConfig {

    /**
     * Weibo API Configuration
     */
    private Weibo weibo = new Weibo();

    /**
     * Baidu Pan API Configuration
     */
    private BaiduPan baiduPan = new BaiduPan();

    /**
     * GitHub Configuration
     */
    private GitHub github = new GitHub();

    @Data
    public static class Weibo {
        private String rssUrl;
    }

    @Data
    public static class BaiduPan {
        private String appId;
        private String appKey;
        private String appSecret;
        private String accessToken;
        private String refreshToken;
        private String redirectUri;
        private String rootDir = "/";
    }

    @Data
    public static class GitHub {
        private String username;
    }

    public Weibo getWeibo() { return weibo; }
    public void setWeibo(Weibo weibo) { this.weibo = weibo; }
    public BaiduPan getBaiduPan() { return baiduPan; }
    public void setBaiduPan(BaiduPan baiduPan) { this.baiduPan = baiduPan; }
    public GitHub getGithub() { return github; }
    public void setGithub(GitHub github) { this.github = github; }
}
