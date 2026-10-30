
package com.nbd.payments.payment.dto;

import com.nbd.payments.payment.entity.Payment;
import java.time.OffsetDateTime;

public record PaymentSummaryResponse(
        String paymentId,
        Long amountPaise,
        String currency,
        String status,
        String errorCode,
        OffsetDateTime createdAt
) {
    public static PaymentSummaryResponse from(Payment payment) {
        return new PaymentSummaryResponse(
                payment.getPaymentId(),
                payment.getAmountPaise(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getErrorCode(),
                payment.getCreatedAt()
        );
    }
}
