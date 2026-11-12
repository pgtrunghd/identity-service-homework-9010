package com.example.identity.service;

import com.example.identity.dto.RegisterRequest;
import com.example.identity.dto.UserResponse;
import com.example.identity.entity.User;
import com.example.identity.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.CONFLICT;

@Service
public class AuthService {
    private final com.example.identity.util.JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository users;
    private final com.example.identity.repository.RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, com.example.identity.util.JwtUtil jwtUtil, RefreshTokenService refreshTokenService, com.example.identity.repository.RefreshTokenRepository refreshTokens) {
        this.refreshTokenService = refreshTokenService;
        this.refreshTokens = refreshTokens;
        this.jwtUtil = jwtUtil;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public com.example.identity.dto.TokenResponseDTO refreshToken(String requestToken) {
        var oldToken = refreshTokens.findWithLockByToken(requestToken).orElseThrow(() ->
            new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Invalid refresh token"));
        if (!oldToken.getExpiryDate().isAfter(java.time.Instant.now())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Refresh token expired");
        }

        var user = oldToken.getUser();
        refreshTokens.delete(oldToken);
        refreshTokens.flush();
        var accessToken = jwtUtil.generateAccessToken(user);
        var refreshToken = refreshTokenService.createRefreshToken(user.getId()).getToken();
        return new com.example.identity.dto.TokenResponseDTO(accessToken, refreshToken);
    }

    public com.example.identity.dto.LoginResponse login(com.example.identity.dto.LoginRequest request) {
        var user = users.findByUsername(request.username()).orElseThrow(() ->
            new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Bad credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Bad credentials");
        }
        return new com.example.identity.dto.LoginResponse(jwtUtil.generateAccessToken(user),
            refreshTokenService.createRefreshToken(user.getId()).getToken());
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.username().trim();
        if (users.existsByUsername(username)) {
            throw new ResponseStatusException(CONFLICT, "Username already exists");
        }
        User user = users.save(new User(username, passwordEncoder.encode(request.password()), "USER"));
        return UserResponse.from(user);
    }
}
