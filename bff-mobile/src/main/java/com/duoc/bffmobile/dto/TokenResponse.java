package com.duoc.bffmobile.dto;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}