
package com.nbd.payments.payment.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "payment_attempts",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_payment_attempt_number",
            columnNames = {"payment_id", "attempt_number"}
        )
    }
)
public class PaymentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
        name = "attempt_id",
        nullable = false,
        unique = true,
        length = 50
    )
    private String attemptId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "payment_id",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_payment_attempts_payment"
        )
    )
    private Payment payment;

    @Column(name = "attempt_number", nullable = false)
    private Short attemptNumber;

    @Column(name = "amount_paise", nullable = false)
    private Long amountPaise;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected PaymentAttempt() {
    }

    public PaymentAttempt(
        String attemptId,
        Payment payment,
        Short attemptNumber,
        Long amountPaise,
        String status,
        String errorCode
    ){
        this.attemptId = attemptId;
        this.payment = payment; 
        this.attemptNumber = attemptNumber;
        this.amountPaise = amountPaise;
        this.status = status;
        this.errorCode = errorCode;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(String attemptId) {
        this.attemptId = attemptId;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public Short getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Short attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public Long getAmountPaise() {
        return amountPaise;
    }

    public void setAmountPaise(Long amountPaise) {
        this.amountPaise = amountPaise;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}