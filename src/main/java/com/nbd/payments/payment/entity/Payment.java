
package com.nbd.payments.payment.entity;

import com.nbd.payments.merchant.entity.Merchant;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false, unique = true, length = 40)
    private String paymentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(name = "checkout_token", nullable = false, unique = true, length = 64)
    private String checkoutToken;

    @Column(name = "amount_paise", nullable = false)
    private Long amountPaise;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "settled_amount_paise", nullable = false)
    private Long settledAmountPaise = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Payment() {}

    public Payment(
            String paymentId,
            Merchant merchant,
            String checkoutToken,
            Long amountPaise
    ) {
        this.paymentId = paymentId;
        this.merchant = merchant;
        this.checkoutToken = checkoutToken;
        this.amountPaise = amountPaise;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void markSuccess() {
        this.status = "SUCCESS";
        this.errorCode = null;
        this.settledAmountPaise = this.amountPaise;
    }

    public void markFailed(String errorCode) {
        this.status = "FAILED";
        this.errorCode = errorCode;
        this.settledAmountPaise = 0L;
    }

    public void markRefunded(){
        if (!"SUCCESS".equals(this.status)) {
            throw new IllegalStateException(
                "Only successful payment can be refunded"
            );
        }

        this.status = "REFUNDED";
        this.errorCode = null;
        this.settledAmountPaise = 0L;        
    }


    public Long getId() {
        return id; 
    }
    public String getPaymentId() { 
        return paymentId; 
    }
    public Merchant getMerchant() { 
        return merchant; 
    }
    public String getCheckoutToken() { 
        return checkoutToken; 
    }
    public Long getAmountPaise() { 
        return amountPaise; 
    }
    public String getCurrency() { 
        return currency; 
    }
    public String getStatus() { 
        return status; 
    }
    public String getErrorCode() { 
        return errorCode; 
    }
    public Long getSettledAmountPaise() { 
        return settledAmountPaise; 
    }
    public OffsetDateTime getCreatedAt() { 
        return createdAt; 
    }
    public OffsetDateTime getUpdatedAt() { 
        return updatedAt; 
    }
}
