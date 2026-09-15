package com.duoc.bffatm.controller;

import com.duoc.bffatm.dto.AuthRequest;
import com.duoc.bffatm.dto.TokenResponse;
import com.duoc.bffatm.service.JwtTokenService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService) {

        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> generarToken(
            @RequestBody AuthRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.username(),
                                request.password()
                        )
                );

        String token = jwtTokenService.generarToken(authentication);

        return ResponseEntity.ok(
                new TokenResponse(
                        token,
                        "Bearer",
                        jwtTokenService.getExpirationSeconds()
                )
        );
    }
}