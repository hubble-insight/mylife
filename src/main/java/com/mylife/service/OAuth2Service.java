package com.mylife.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mylife.config.ApiConfig;
import com.mylife.model.OAuthToken;
import com.mylife.repository.OAuthTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class OAuth2Service {

    private static final String AUTHORIZE_URL = "https://openapi.baidu.com/oauth/2.0/authorize";
    private static final String TOKEN_URL = "https://openapi.baidu.com/oauth/2.0/token";
    private static final String REVOKE_URL = "https://openapi.baidu.com/rest/2.0/passport/auth/revokeAuthorization";

    private final RestTemplate restTemplate;
    private final ApiConfig apiConfig;
    private final OAuthTokenRepository tokenRepository;
    private final Gson gson;

    public OAuth2Service(RestTemplate restTemplate, ApiConfig apiConfig,
                         OAuthTokenRepository tokenRepository, Gson gson) {
        this.restTemplate = restTemplate;
        this.apiConfig = apiConfig;
        this.tokenRepository = tokenRepository;
        this.gson = gson;
    }

    /**
     * 生成授权 URL
     * @param redirectUri 回调地址
     * @param state 随机状态字符串
     * @return 授权 URL
     */
    public String getAuthorizationUrl(String redirectUri, String state) {
        String appId = apiConfig.getBaiduPan().getAppId();
        String appKey = apiConfig.getBaiduPan().getAppKey();

        // 百度网盘需要的 scope：basic 和 netdisk
        String scope = "basic,netdisk";

        log.info("生成百度网盘授权 URL，appId: {}, redirectUri: {}", appId, redirectUri);

        return UriComponentsBuilder.fromHttpUrl(AUTHORIZE_URL)
            .queryParam("response_type", "code")
            .queryParam("client_id", appKey)
            .queryParam("device_id", appId)
            .queryParam("redirect_uri", redirectUri)
            .queryParam("scope", scope)
            .queryParam("state", state)
            .toUriString();
    }

    /**
     * 使用授权码换取 token
     * @param code 授权码
     * @param redirectUri 回调地址
     * @return OAuthToken
     */
    @Transactional
    public OAuthToken exchangeToken(String code, String redirectUri) {
        String appKey = apiConfig.getBaiduPan().getAppKey();
        String appSecret = apiConfig.getBaiduPan().getAppSecret();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("client_id", appKey);
        body.add("client_secret", appSecret);
        body.add("redirect_uri", redirectUri);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                TOKEN_URL, body, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonObject json = gson.fromJson(response.getBody(), JsonObject.class);

                if (json.has("error")) {
                    throw new RuntimeException("换取 Token 失败：" + json.get("error_description").getAsString());
                }

                OAuthToken token = parseToken(json);
                tokenRepository.save(token);
                log.info("成功换取百度网盘 OAuth Token");
                return token;
            }
        } catch (Exception e) {
            log.error("换取 Token 失败：{}", e.getMessage());
            throw new RuntimeException("换取 Token 失败：" + e.getMessage());
        }

        throw new RuntimeException("换取 Token 失败");
    }

    /**
     * 使用 refresh_token 刷新 access_token
     * @return 刷新后的 OAuthToken
     */
    @Transactional
    public OAuthToken refreshToken() {
        Optional<OAuthToken> optionalToken = tokenRepository.findById("baidu_pan");

        if (optionalToken.isEmpty() || optionalToken.get().getRefreshToken() == null) {
            throw new IllegalStateException("未找到 refresh_token，请先进行授权");
        }

        OAuthToken oldToken = optionalToken.get();
        String appKey = apiConfig.getBaiduPan().getAppKey();
        String appSecret = apiConfig.getBaiduPan().getAppSecret();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", oldToken.getRefreshToken());
        body.add("client_id", appKey);
        body.add("client_secret", appSecret);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                TOKEN_URL, body, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonObject json = gson.fromJson(response.getBody(), JsonObject.class);

                if (json.has("error")) {
                    throw new RuntimeException("刷新 Token 失败：" + json.get("error_description").getAsString());
                }

                OAuthToken newToken = parseToken(json);
                // 保留 refresh_token
                newToken.setRefreshToken(oldToken.getRefreshToken());
                tokenRepository.save(newToken);
                log.info("成功刷新百度网盘 OAuth Token");
                return newToken;
            }
        } catch (Exception e) {
            log.error("刷新 Token 失败：{}", e.getMessage());
            throw new RuntimeException("刷新 Token 失败：" + e.getMessage());
        }

        throw new RuntimeException("刷新 Token 失败");
    }

    /**
     * 获取存储的 token，如果过期则自动刷新
     * @return OAuthToken
     */
    @Transactional
    public OAuthToken getValidToken() {
        Optional<OAuthToken> optionalToken = tokenRepository.findById("baidu_pan");

        if (optionalToken.isEmpty() || !optionalToken.get().isAuthorized()) {
            throw new IllegalStateException("未授权，请先进行 OAuth 授权");
        }

        OAuthToken token = optionalToken.get();

        if (token.isExpired()) {
            log.info("Token 已过期，自动刷新...");
            return refreshToken();
        }

        return token;
    }

    /**
     * 获取当前授权状态
     */
    public Optional<OAuthToken> getAuthorizationStatus() {
        return tokenRepository.findById("baidu_pan");
    }

    /**
     * 撤销授权
     */
    @Transactional
    public void revokeAuthorization() {
        tokenRepository.deleteById("baidu_pan");
        log.info("已撤销百度网盘授权");
    }

    private OAuthToken parseToken(JsonObject json) {
        OAuthToken token = new OAuthToken();
        token.setId("baidu_pan");
        token.setAccessToken(json.has("access_token") ? json.get("access_token").getAsString() : null);
        token.setRefreshToken(json.has("refresh_token") ? json.get("refresh_token").getAsString() : null);
        token.setExpiresIn(json.has("expires_in") ? json.get("expires_in").getAsLong() : null);
        token.setTokenType(json.has("token_type") ? json.get("token_type").getAsString() : null);
        token.setScope(json.has("scope") ? json.get("scope").getAsString() : null);
        token.setAuthorizedAt(LocalDateTime.now());

        if (token.getExpiresIn() != null) {
            token.setExpiresAt(LocalDateTime.now().plusSeconds(token.getExpiresIn()));
        }
        token.setUpdatedAt(LocalDateTime.now());

        return token;
    }
}
