package com.nbd.payments.merchant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record MerchantRegistrationRequest(
    @NotBlank (message = "Business name is required")
    @Size (max = 150, message = "Business name must not exceed 150 characters")
    String businessName,

    @NotBlank (message = "Email is required")
    @Email (message = "Invalid email address")
    @Size (max = 254, message = "Email must not exceed 254 characters")
    String email,

    @NotBlank (message = "Password is required")
    @Size (
        min = 8,
        max = 72,
        message = "Password must be between * and 72 characters"
    )
    String password
) {}
