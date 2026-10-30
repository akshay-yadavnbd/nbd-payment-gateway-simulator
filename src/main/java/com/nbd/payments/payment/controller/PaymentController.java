
package com.nbd.payments.payment.controller;

import com.nbd.payments.merchant.entity.Merchant;
import com.nbd.payments.merchant.service.MerchantApiKeyService;
import com.nbd.payments.payment.dto.CardPaymentRequest;
import com.nbd.payments.payment.dto.CreatePaymentRequest;
import com.nbd.payments.payment.dto.PaymentResponse;
import com.nbd.payments.payment.service.PaymentService;

import jakarta.validation.Valid;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            @Valid @RequestBody CreatePaymentRequest request
    ) {

        Merchant merchant = apiKeyService.authenticateApiKey(apiKey);

        PaymentResponse response = paymentService.createPayment(
                merchant,
                request.amountPaise()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

        
    @PostMapping("/checkout/{checkoutToken}/pay")
    public ResponseEntity<PaymentResponse> pay(
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

}
