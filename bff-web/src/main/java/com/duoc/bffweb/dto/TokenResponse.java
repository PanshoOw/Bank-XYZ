package com.duoc.bffweb.dto;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}