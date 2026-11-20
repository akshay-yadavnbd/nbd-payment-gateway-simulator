
package com.nbd.payments.auth.controller;

import com.nbd.payments.auth.dto.ForgotPasswordRequest;
import com.nbd.payments.auth.dto.ResetPasswordRequest;
import com.nbd.payments.auth.service.MerchantPasswordRecoveryService;

import jakarta.validation.Valid;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/merchant")
public class MerchantPasswordRecoveryController {

    private final MerchantPasswordRecoveryService recoveryService;

    public MerchantPasswordRecoveryController(
            MerchantPasswordRecoveryService recoveryService
    ) {
        this.recoveryService = recoveryService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        recoveryService.requestPasswordReset(request.email());

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "If an account exists for that email, a password reset link will be sent."
                )
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        recoveryService.resetPassword(
                request.token(),
                request.newPassword()
        );

        return ResponseEntity.ok(
                Map.of("message", "Password has been reset successfully.")
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidRequest(
            IllegalArgumentException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", exception.getMessage()));
    }
}
