package com.example.identity.controller;

import com.example.identity.dto.RegisterRequest;
import com.example.identity.dto.UserResponse;
import com.example.identity.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final com.example.identity.repository.UserRepository users;
    private final com.example.identity.util.JwtUtil jwtUtil;

    public AuthController(AuthService authService, com.example.identity.repository.UserRepository users, com.example.identity.util.JwtUtil jwtUtil) {
        this.users = users;
        this.jwtUtil = jwtUtil;
        this.authService = authService;
    }

    @GetMapping(value = "/test-token", produces = "text/plain")
    public String testToken(@RequestParam String username) {
        var user = users.findByUsername(username).orElseThrow(() ->
            new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return jwtUtil.generateToken(user);
    }

    @PostMapping("/login")
    public com.example.identity.dto.LoginResponse login(@Valid @RequestBody com.example.identity.dto.LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public com.example.identity.dto.TokenResponseDTO refresh(@Valid @RequestBody com.example.identity.dto.RefreshTokenRequest request) {
        return authService.refreshToken(request.refreshToken());
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }
}
