package com.nbd.payments.admin.dto;

import jakarta.validation.constraints.NotNull;

public record MerchantStatusRequest(
    @NotNull (message = "Status is required")
    MerchantStatus status
) {}
