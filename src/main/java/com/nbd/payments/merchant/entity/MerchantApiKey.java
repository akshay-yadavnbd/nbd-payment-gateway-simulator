
package com.nbd.payments.merchant.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "merchant_api_keys")
public class MerchantApiKey {

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
        name = "key_hash",
        nullable = false,
        unique = true,
        length = 64
    )
    private String keyHash;

    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private OffsetDateTime createdAt;

    protected MerchantApiKey() {
        // Required by JPA
    }

    public MerchantApiKey(Merchant merchant, String keyHash) {
        this.merchant = merchant;
        this.keyHash = keyHash;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void replaceKeyHash(String newKeyHash) {
        this.keyHash = newKeyHash;
    }
}
