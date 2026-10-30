
package com.nbd.payments.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreatePaymentRequest(

    @NotNull
    @Min(value = 1, message = "Amount must be greater than zero")
    Long amountPaise,

    @NotNull
    @Pattern(regexp = "INR", message = "Only INR is supported")
    String currency

) {}
