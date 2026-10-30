
package com.nbd.payments.payment.dto;

import com.nbd.payments.payment.entity.Payment;

import java.time.OffsetDateTime;

public record PaymentResponse(
        String paymentId,
        String checkoutToken,
        Long amountPaise,
        String currency,
        String status,
        String errorCode,
        OffsetDateTime createdAt
) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getCheckoutToken(),
                payment.getAmountPaise(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getErrorCode(),
                payment.getCreatedAt()
        );
    }
}
