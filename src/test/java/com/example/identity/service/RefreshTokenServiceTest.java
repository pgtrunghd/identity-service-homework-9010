package com.example.identity.service;

import com.example.identity.entity.RefreshToken;
import com.example.identity.entity.User;
import com.example.identity.repository.RefreshTokenRepository;
import com.example.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {
    @Test void createsUuidTokenWithUserAndSevenDayExpiry() {
        var users = mock(UserRepository.class);
        var tokens = mock(RefreshTokenRepository.class);
        var user = new User("alice", "hash", "USER");
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(tokens.save(any(RefreshToken.class))).thenAnswer(call -> call.getArgument(0));
        var before = Instant.now();
        var token = new RefreshTokenService(tokens, users, 604800000).createRefreshToken(user.getId());
        assertEquals(user, token.getUser());
        assertDoesNotThrow(() -> java.util.UUID.fromString(token.getToken()));
        assertFalse(token.getExpiryDate().isBefore(before.plusMillis(604800000)));
        verify(tokens).save(token);
    }

    @Test void rejectsExpiredToken() {
        var tokens = mock(RefreshTokenRepository.class);
        when(tokens.findByToken("expired")).thenReturn(Optional.of(
            new RefreshToken(new User("alice", "hash", "USER"), "expired", Instant.now().minusSeconds(1))));
        var service = new RefreshTokenService(tokens, mock(UserRepository.class), 604800000);
        assertThrows(ResponseStatusException.class, () -> service.verifyExpiration("expired"));
    }
}
