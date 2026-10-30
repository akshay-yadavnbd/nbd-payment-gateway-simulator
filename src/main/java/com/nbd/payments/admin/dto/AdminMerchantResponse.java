package com.nbd.payments.admin.dto;

import java.time.OffsetDateTime;

import com.nbd.payments.merchant.entity.Merchant;

public record AdminMerchantResponse(
    String merchantId,
    String businessName,
    String email,
    String status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public static AdminMerchantResponse from(Merchant merchant) {
        return new AdminMerchantResponse(
                merchant.getMerchantId(),
                merchant.getBusinessName(),
                merchant.getEmail(),
                merchant.getStatus(),
                merchant.getCreatedAt(),
                merchant.getUpdatedAt()
        );
    }
}
