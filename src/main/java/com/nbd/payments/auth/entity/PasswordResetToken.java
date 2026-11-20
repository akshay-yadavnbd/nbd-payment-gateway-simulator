
package com.nbd.payments.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "credential_id", nullable = false)
    private Long credentialId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PasswordResetToken() {
    }

    public PasswordResetToken(
            Long credentialId,
            String tokenHash,
            Instant expiresAt
    ) {
        this.credentialId = credentialId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getCredentialId() {
        return credentialId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getInvalidatedAt() {
        return invalidatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void markUsed() {
        this.usedAt = Instant.now();
    }

    public void invalidate() {
        this.invalidatedAt = Instant.now();
    }

    public boolean isUsable(Instant now) {
        return usedAt == null
                && invalidatedAt == null
                && expiresAt.isAfter(now);
    }
}
