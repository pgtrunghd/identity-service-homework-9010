package com.example.identity;

import com.example.identity.controller.AuthController;
import com.example.identity.config.SecurityConfig;
import com.example.identity.service.AuthService;
import com.example.identity.util.JwtUtil;
import com.example.identity.entity.User;
import com.example.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@WebMvcTest(controllers = AuthController.class, properties = {"app.jwt.secret=identity-service-demo-key-256-bits!", "app.jwt.access-token-expiration=900000"})
@Import({SecurityConfig.class, AuthService.class, JwtUtil.class})
class LoginApiTest {
    @Autowired MockMvc mvc;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
    @MockitoBean UserRepository users;
    @MockitoBean com.example.identity.service.RefreshTokenService refreshTokenService;
    @MockitoBean com.example.identity.repository.RefreshTokenRepository refreshTokens;

    @Test void validLoginReturnsJwtWithoutSessionOrCookie() throws Exception {
        when(users.findByUsername("alice")).thenReturn(Optional.of(new User("alice", encoder.encode("secret123"), "USER")));
        when(refreshTokenService.createRefreshToken(anyString())).thenReturn(new com.example.identity.entity.RefreshToken(
            new User("alice", "hash", "USER"), "refresh-id", java.time.Instant.now().plusSeconds(3600)));
        var result = mvc.perform(post("/api/auth/login").contentType("application/json")
            .content("{\"username\":\"alice\",\"password\":\"secret123\"}"))
            .andExpect(status().isOk()).andExpect(header().doesNotExist("Set-Cookie")).andReturn();
        assertNull(result.getRequest().getSession(false));
        var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(result.getResponse().getContentAsString());
        assertEquals(3, json.get("accessToken").asText().split("\\.").length);
        assertEquals("refresh-id", json.get("refreshToken").asText());
    }

    @Test void wrongPasswordAndUnknownUserReturnSameUnauthorizedError() throws Exception {
        when(users.findByUsername("alice")).thenReturn(Optional.of(new User("alice", encoder.encode("secret123"), "USER")));
        for (String name : new String[]{"alice", "unknown"}) {
            var result = mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\"" + name + "\",\"password\":\"incorrect\"}"))
                .andExpect(status().isUnauthorized()).andExpect(header().doesNotExist("Set-Cookie")).andReturn();
            assertEquals("Bad credentials", result.getResponse().getErrorMessage());
            assertNull(result.getRequest().getSession(false));
        }
    }
}
