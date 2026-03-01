package com.mylife.controller;

import com.mylife.config.ApiConfig;
import com.mylife.model.OAuthToken;
import com.mylife.service.OAuth2Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/oauth/baidu")
public class OAuthController {

    private final OAuth2Service oauth2Service;
    private final ApiConfig apiConfig;

    public OAuthController(OAuth2Service oauth2Service, ApiConfig apiConfig) {
        this.oauth2Service = oauth2Service;
        this.apiConfig = apiConfig;
    }

    /**
     * 授权状态页面
     */
    @GetMapping("/status")
    public String authorizationStatus(Model model) {
        Optional<OAuthToken> token = oauth2Service.getAuthorizationStatus();

        model.addAttribute("authorized", token.isPresent() && token.get().isAuthorized());
        model.addAttribute("token", token.orElse(null));

        return "oauth/status";
    }

    /**
     * 开始授权流程
     */
    @GetMapping("/authorize")
    public RedirectView startAuthorization() {
        String redirectUri = getRedirectUri();
        String state = UUID.randomUUID().toString().replace("-", "");

        // 将 state 存入 session 用于验证（这里简化处理）
        String authUrl = oauth2Service.getAuthorizationUrl(redirectUri, state);

        log.info("跳转到百度网盘授权页面：{}", authUrl);
        return new RedirectView(authUrl);
    }

    /**
     * OAuth 回调处理
     */
    @GetMapping("/callback")
    public String handleCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription,
            Model model) {

        if (error != null) {
            model.addAttribute("success", false);
            model.addAttribute("message", "授权失败：" + errorDescription);
            return "oauth/callback";
        }

        if (code == null) {
            model.addAttribute("success", false);
            model.addAttribute("message", "未收到授权码");
            return "oauth/callback";
        }

        try {
            String redirectUri = getRedirectUri();
            oauth2Service.exchangeToken(code, redirectUri);

            model.addAttribute("success", true);
            model.addAttribute("message", "授权成功！");
        } catch (Exception e) {
            log.error("换取 Token 失败", e);
            model.addAttribute("success", false);
            model.addAttribute("message", "换取 Token 失败：" + e.getMessage());
        }

        return "oauth/callback";
    }

    /**
     * 刷新 Token
     */
    @PostMapping("/refresh")
    public RedirectView refreshToken() {
        try {
            oauth2Service.refreshToken();
            log.info("Token 刷新成功");
        } catch (Exception e) {
            log.error("Token 刷新失败", e);
        }
        return new RedirectView("/oauth/baidu/status");
    }

    /**
     * 撤销授权
     */
    @PostMapping("/revoke")
    public RedirectView revokeAuthorization() {
        oauth2Service.revokeAuthorization();
        return new RedirectView("/oauth/baidu/status");
    }

    /**
     * 测试百度网盘 API 连接
     */
    @GetMapping("/test")
    @ResponseBody
    public String testBaiduPanApi() {
        try {
            OAuthToken token = oauth2Service.getValidToken();
            String testUrl = "https://pan.baidu.com/rest/2.0/xpan/file?method=list&access_token="
                + token.getAccessToken() + "&dir=/&limit=1";

            org.springframework.web.client.RestTemplate restTemplate =
                new org.springframework.web.client.RestTemplate();

            String response = restTemplate.getForObject(testUrl, String.class);
            return "测试成功！<br/>请求 URL: " + testUrl + "<br/>响应：" + response;
        } catch (Exception e) {
            return "测试失败：" + e.getMessage();
        }
    }

    private String getRedirectUri() {
        String redirectUri = apiConfig.getBaiduPan().getRedirectUri();
        log.debug("Using redirect URI: {}", redirectUri);
        return redirectUri;
    }
}
