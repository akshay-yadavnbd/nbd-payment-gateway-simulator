
package com.nbd.payments.payment.dto;

import com.nbd.payments.payment.entity.Payment;

public record MerchantPaymentStatusResponse(
        String paymentId,
        Long amountPaise,
        String currency,
        String status,
        String errorCode
) {
    public static MerchantPaymentStatusResponse from(Payment payment) {
        return new MerchantPaymentStatusResponse(
                payment.getPaymentId(),
                payment.getAmountPaise(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getErrorCode()
        );
    }
}
