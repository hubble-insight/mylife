package com.mylife.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * OAuth2 Token Entity - 存储百度网盘授权信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "oauth_tokens")
public class OAuthToken {

    @Id
    @Column(name = "id")
    private String id = "baidu_pan"; // 单例，固定 ID

    @Column(name = "access_token", length = 1000)
    private String accessToken;

    @Column(name = "refresh_token", length = 1000)
    private String refreshToken;

    @Column(name = "expires_in")
    private Long expiresIn;

    @Column(name = "token_type")
    private String tokenType;

    @Column(name = "scope", length = 500)
    private String scope;

    @Column(name = "authorized_at")
    private LocalDateTime authorizedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 检查 token 是否过期（提前 5 分钟刷新）
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return true;
        }
        return LocalDateTime.now().plusMinutes(5).isAfter(expiresAt);
    }

    /**
     * 检查是否已授权
     */
    public boolean isAuthorized() {
        return accessToken != null && !accessToken.isEmpty();
    }
}
