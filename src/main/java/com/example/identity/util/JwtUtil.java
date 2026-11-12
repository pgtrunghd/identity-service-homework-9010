package com.example.identity.util;

import com.example.identity.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;

@Component
public class JwtUtil {
    private final SecretKey key;
    private final long expiration;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.access-token-expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        if (expiration <= 0) throw new IllegalArgumentException("Expiration must be positive");
        this.expiration = expiration;
    }

    public String generateAccessToken(User user) {
        Date now = new Date();
        return Jwts.builder().setId(java.util.UUID.randomUUID().toString()).setSubject(user.getUsername())
            .claim("roles", java.util.List.of(user.getRole())).setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + expiration))
            .signWith(key, SignatureAlgorithm.HS256).compact();
    }

    public String generateToken(User user) { return generateAccessToken(user); }
}
