package com.nbd.payments.payment.dto;

public record CardPaymentResponse(

        String paymentId,

        String attemptId,

        String status,

        String attemptStatus,

        String errorCode,

        String message,

        int attemptNumber,

        int attemptsRemaining

) {
}