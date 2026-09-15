package com.duoc.bffatm.service;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final String scope;
    private final long expirationSeconds;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            @Value("${bff.jwt.issuer}") String issuer,
            @Value("${bff.jwt.scope}") String scope,
            @Value("${bff.jwt.expiration-seconds}") long expirationSeconds) {

        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.scope = scope;
        this.expirationSeconds = expirationSeconds;
    }

    public String generarToken(Authentication authentication) {

        Instant ahora = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(expirationSeconds))
                .subject(authentication.getName())
                .claim("scope", scope)
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}