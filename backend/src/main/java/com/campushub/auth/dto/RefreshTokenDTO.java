package com.campushub.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * POST /api/auth/token/refresh  请求体。
 */
public class RefreshTokenDTO {

    @NotBlank
    private String refreshToken;

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
