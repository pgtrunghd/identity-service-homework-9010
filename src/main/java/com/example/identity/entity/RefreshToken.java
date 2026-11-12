package com.example.identity.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens", indexes = @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"))
public class RefreshToken {
    @Id
    private String id;

    @Column(nullable = false, unique = true, length = 36)
    private String token;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    protected RefreshToken() {}

    public RefreshToken(User user, String token, Instant expiryDate) {
        this.id = UUID.randomUUID().toString();
        this.user = user;
        this.token = token;
        this.expiryDate = expiryDate;
    }

    public String getId() { return id; }
    public String getToken() { return token; }
    public Instant getExpiryDate() { return expiryDate; }
    public User getUser() { return user; }
}
