package com.example.identity.service;

import com.example.identity.dto.TokenResponseDTO;
import com.example.identity.entity.RefreshToken;
import com.example.identity.entity.User;
import com.example.identity.repository.RefreshTokenRepository;
import com.example.identity.repository.UserRepository;
import com.example.identity.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TokenRotationTest {
    private final UserRepository users = mock(UserRepository.class);
    private final RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
    private final JwtUtil jwt = mock(JwtUtil.class);
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final AuthService auth = new AuthService(users, mock(PasswordEncoder.class), jwt, refreshTokenService, tokens);

    @Test void rotatesTokenAndDeletesOldOneBeforeCreatingNewOne() {
        var user = new User("alice", "hash", "USER");
        var old = new RefreshToken(user, "r1", Instant.now().plusSeconds(60));
        var next = new RefreshToken(user, "r2", Instant.now().plusSeconds(120));
        when(tokens.findWithLockByToken("r1")).thenReturn(Optional.of(old));
        when(jwt.generateAccessToken(user)).thenReturn("access-b");
        when(refreshTokenService.createRefreshToken(user.getId())).thenReturn(next);

        TokenResponseDTO response = auth.refreshToken("r1");

        assertEquals("access-b", response.accessToken());
        assertEquals("r2", response.refreshToken());
        var order = inOrder(tokens, jwt, refreshTokenService);
        order.verify(tokens).findWithLockByToken("r1");
        order.verify(tokens).delete(old);
        order.verify(tokens).flush();
        order.verify(jwt).generateAccessToken(user);
        order.verify(refreshTokenService).createRefreshToken(user.getId());
    }

    @Test void rejectsMissingAndExpiredTokensWithForbidden() {
        var missing = assertThrows(ResponseStatusException.class, () -> auth.refreshToken("missing"));
        assertEquals(HttpStatus.FORBIDDEN, missing.getStatusCode());

        var expired = new RefreshToken(new User("alice", "hash", "USER"), "expired", Instant.now().minusSeconds(1));
        when(tokens.findWithLockByToken("expired")).thenReturn(Optional.of(expired));
        var exception = assertThrows(ResponseStatusException.class, () -> auth.refreshToken("expired"));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(tokens, never()).delete(any());
        verifyNoInteractions(jwt, refreshTokenService);
    }
}
