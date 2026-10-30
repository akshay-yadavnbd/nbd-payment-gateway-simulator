
package com.nbd.payments.merchant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "merchant_credentials")
public class MerchantCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "merchant_id",
        referencedColumnName = "id",
        nullable = false,
        unique = true
    )
    private Merchant merchant;

    @Column(
        name = "password_hash",
        nullable = false,
        length = 255
    )
    private String passwordHash;

    @Column(
        name = "failed_login_attempts",
        nullable = false
    )
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(
        name = "password_changed_at",
        nullable = false
    )
    private OffsetDateTime passwordChangedAt;

    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(
        name = "updated_at",
        nullable = false
    )
    private OffsetDateTime updatedAt;

    protected MerchantCredential() {
        // Required by JPA
    }

    public MerchantCredential(
            Merchant merchant,
            String passwordHash
    ) {
        if (merchant == null) {
            throw new IllegalArgumentException(
                "Merchant is required"
            );
        }

        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException(
                "Password hash is required"
            );
        }

        this.merchant = merchant;
        this.passwordHash = passwordHash;
        this.failedLoginAttempts = 0;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        this.createdAt = now;
        this.updatedAt = now;

        if (this.passwordChangedAt == null) {
            this.passwordChangedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Integer getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public OffsetDateTime getLockedUntil() {
        return lockedUntil;
    }

    public OffsetDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void changePasswordHash(String newPasswordHash) {

        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new IllegalArgumentException(
                "Password hash is required"
            );
        }

        this.passwordHash = newPasswordHash;
        this.passwordChangedAt =
                OffsetDateTime.now(ZoneOffset.UTC);

        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    public void recordFailedLogin(OffsetDateTime lockUntil) {
        this.failedLoginAttempts++;
        this.lockedUntil = lockUntil;
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }
}
