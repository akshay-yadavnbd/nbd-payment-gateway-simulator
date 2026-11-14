
package com.nbd.payments.email.controller;

import com.nbd.payments.email.service.EmailVerificationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/email-verification")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(
            EmailVerificationService emailVerificationService
    ) {
        this.emailVerificationService = emailVerificationService;
    }

    @GetMapping("/verify")
    public ResponseEntity<Void> verifyEmail(
            @RequestParam(required = false) String token
    ) {
        String status;
        try {
            emailVerificationService.verifyEmail(token);

            status = "success";
        } catch (IllegalArgumentException exception){
                status = "invalid";
        }
        URI redirectUri = URI.create(
            "/email-verification.html?status=" + status
        );

            return ResponseEntity
                        .status(HttpStatus.SEE_OTHER)
                        .location(redirectUri)
                        .build();
    }
}
