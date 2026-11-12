package com.example.identity.service;

import com.example.identity.entity.RefreshToken;
import com.example.identity.repository.RefreshTokenRepository;
import com.example.identity.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokens;
    private final UserRepository users;
    private final long expirationMillis;

    public RefreshTokenService(RefreshTokenRepository refreshTokens, UserRepository users,
            @Value("${app.jwt.refresh-token-expiration}") long expirationMillis) {
        if (expirationMillis <= 0) throw new IllegalArgumentException("Refresh token expiration must be positive");
        this.refreshTokens = refreshTokens;
        this.users = users;
        this.expirationMillis = expirationMillis;
    }

    @Transactional
    public RefreshToken createRefreshToken(String userId) {
        var user = users.findById(userId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return refreshTokens.save(new RefreshToken(user, UUID.randomUUID().toString(),
            Instant.now().plusMillis(expirationMillis)));
    }

    @Transactional(readOnly = true)
    public RefreshToken verifyExpiration(String token) {
        var refreshToken = refreshTokens.findByToken(token).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        if (!refreshToken.getExpiryDate().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }
        return refreshToken;
    }
}
