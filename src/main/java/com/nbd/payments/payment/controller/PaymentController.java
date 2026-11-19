
package com.nbd.payments.payment.controller;

import com.nbd.payments.merchant.entity.Merchant;
import com.nbd.payments.merchant.service.MerchantApiKeyService;
import com.nbd.payments.payment.dto.CardPaymentRequest;
import com.nbd.payments.payment.dto.CardPaymentResponse;
import com.nbd.payments.payment.dto.CreatePaymentRequest;
import com.nbd.payments.payment.dto.PaymentResponse;
import com.nbd.payments.payment.service.PaymentService;
import com.nbd.payments.payment.dto.MerchantPaymentStatusResponse;

import jakarta.validation.Valid;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

        private final PaymentService paymentService;
        private final MerchantApiKeyService apiKeyService;

        public PaymentController(
                PaymentService paymentService,
                MerchantApiKeyService apiKeyService
        ) {
                this.paymentService = paymentService;
                this.apiKeyService = apiKeyService;
        }

        @PostMapping
        public ResponseEntity<PaymentResponse> createPayment(
                @RequestHeader("X-API-Key") String apiKey,
                @RequestHeader (
                                value =  "Idempotency-key", 
                                required = false
                                ) String idempotencyKey,
                @Valid @RequestBody CreatePaymentRequest request
        ) {
                if (idempotencyKey == null || idempotencyKey.isBlank()) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Idempotency key header is required "
                        );
                }
        Merchant apiKeyMerchant =
                apiKeyService.authenticateApiKey(apiKey);
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_MERCHANT"))) {
                String loggedInMerchantId = authentication.getName();
                if (!loggedInMerchantId.equals(
                        apiKeyMerchant.getMerchantId())) {

                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "API key is not valid for the logged-in merchant"
                        );
                }
        }
        PaymentResponse response = paymentService.createPayment(
                apiKeyMerchant,
                request.amountPaise(),
                idempotencyKey,
                request.returnUrl()
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
        }
        
        @PostMapping("/checkout/{checkoutToken}/pay")
        public ResponseEntity<CardPaymentResponse> pay(
                @PathVariable String checkoutToken,
                @Valid @RequestBody CardPaymentRequest request
        ) {
                return ResponseEntity.ok(
                        paymentService.processCardPayment(
                                checkoutToken,
                                request
                        )
                );
        }

        @GetMapping("/checkout/{checkoutToken}")
        public ResponseEntity<Map<String, Object>> checkoutDetails(
                @PathVariable String checkoutToken
        ) {
                return ResponseEntity.ok(
                        paymentService.getCheckoutDetails(checkoutToken)
                );
        }
        @GetMapping("/{paymentId}")
        public ResponseEntity<MerchantPaymentStatusResponse> getPaymentStatus(
                @RequestHeader("X-API-Key") String apiKey,
                @PathVariable String paymentId
        ) {
        Merchant merchant = apiKeyService.authenticateApiKey(apiKey);

        return ResponseEntity.ok(
                paymentService.getMerchantPaymentStatus(merchant, paymentId)
        );
        }
}
