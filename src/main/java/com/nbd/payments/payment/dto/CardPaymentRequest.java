
package com.nbd.payments.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record CardPaymentRequest(
        @NotBlank (message = "Card number can't be blank")
        String cardNumber,
        @NotBlank (message = "Card expiry is required")
        String expiry,
        @NotBlank (message = "CVV can't be blank")
        String cvv
) {}
