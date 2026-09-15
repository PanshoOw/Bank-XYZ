package com.duoc.bffweb.dto;

public record AuthRequest(
        String username,
        String password
) {
}