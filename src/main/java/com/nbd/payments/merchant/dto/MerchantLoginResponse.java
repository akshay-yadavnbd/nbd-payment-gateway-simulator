package com.nbd.payments.merchant.dto;

public record MerchantLoginResponse(
    String merchantId,
    String businessName,
    String status,
    String message
) {}
