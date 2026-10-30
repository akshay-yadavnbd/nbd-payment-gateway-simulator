package com.nbd.payments.merchant.dto;
import java.time.OffsetDateTime;

public record MerchantRegistrationResponse (
    String merchantId,
    String businessName,
    String email,
    String status,
    OffsetDateTime createdAt
){}
