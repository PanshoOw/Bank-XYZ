package com.duoc.bffatm.dto;

public record AuthRequest(
        String username,
        String password
) {
}