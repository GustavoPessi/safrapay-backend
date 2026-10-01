package com.safrapay.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de POST /api/auth/refresh e POST /api/auth/logout. */
public class RefreshRequest {

    @NotBlank(message = "refreshToken eh obrigatorio")
    @Size(max = 200, message = "refreshToken invalido")
    private String refreshToken;

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
