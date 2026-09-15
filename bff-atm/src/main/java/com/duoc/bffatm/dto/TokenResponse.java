package com.duoc.bffatm.dto;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}