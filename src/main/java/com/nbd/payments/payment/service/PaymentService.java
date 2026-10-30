
package com.nbd.payments.payment.service;

import com.nbd.payments.merchant.entity.Merchant;
import com.nbd.payments.payment.dto.CardPaymentRequest;
import com.nbd.payments.payment.dto.PaymentResponse;
import com.nbd.payments.payment.entity.Payment;
import com.nbd.payments.payment.repository.PaymentRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final PaymentScenarioSimulator simulator;

    
    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentScenarioSimulator simulator
    ) {
        this.paymentRepository = paymentRepository;
        this.simulator = simulator;
    }


    @Transactional
    public PaymentResponse createPayment(
            Merchant merchant,
            Long amountPaise
    ) {

        if (merchant == null || !"ACTIVE".equals(merchant.getStatus())) {
            throw new IllegalArgumentException(
                    "Merchant is not active"
            );
        }

        if (amountPaise == null || amountPaise <= 0) {
            throw new IllegalArgumentException(
                    "Invalid payment amount"
            );
        }

        String paymentId = "PAY-" +
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .toUpperCase();

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String checkoutToken = HexFormat.of()
                .formatHex(randomBytes);

        Payment payment = new Payment(
                paymentId,
                merchant,
                checkoutToken,
                amountPaise
        );

        Payment savedPayment = paymentRepository.saveAndFlush(payment);

        return PaymentResponse.from(savedPayment);
    }

    @Transactional
    public PaymentResponse processCardPayment(
            String checkoutToken,
            CardPaymentRequest request
    ) {

        Payment payment = paymentRepository
                .findWithLockByCheckoutToken(checkoutToken)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Checkout not found"
                        )
                );

        if (!"PENDING".equals(payment.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment already processed"
            );
        }

        if (payment.getCreatedAt().isBefore(
                OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(15)
        )) {
            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "Checkout expired"
            );
        }

        if (!"ACTIVE".equals(payment.getMerchant().getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Merchant is not active"
            );
        }

        String result = simulator.evaluate(
                request.cardNumber(),
                request.expiry(),
                request.cvv()
        );

        if ("SUCCESS".equals(result)) {
            payment.markSuccess();
        } else {
            payment.markFailed(result);
        }

        return PaymentResponse.from(
                paymentRepository.saveAndFlush(payment)
        );
    }

    @Transactional (readOnly = true)
    public List<PaymentResponse> getMerchantTransactions(
        Merchant merchant
    ){
        return paymentRepository
                    .findByMerchant_IdOrderByCreatedAtDesc(merchant.getId())
                    .stream()
                    .map(PaymentResponse::from)
                    .toList();
    }

    @Transactional (readOnly = true)
    public Long getMerchantBalance(Merchant merchant){
        return  paymentRepository.calculateMerchantBalance(
            merchant.getId()
        );
    }
    
    @Transactional
    public PaymentResponse refundPayment(
            Merchant merchant,
            String paymentId
    ) {

        Payment payment = paymentRepository
                .findForRefund(paymentId, merchant.getId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Payment not found"
                        )
                );

        if (!"SUCCESS".equals(payment.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only successful payments can be refunded"
            );
        }

        payment.markRefunded();

        return PaymentResponse.from(
                paymentRepository.saveAndFlush(payment)
        );
    }
  
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllTransactions() {
        return paymentRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(PaymentResponse::from)
                .toList();
    }

    // Merchant name and payment amount on checkout page          
    @Transactional(readOnly = true)
    public Map<String, Object> getCheckoutDetails(String checkoutToken) {

        Payment payment = paymentRepository
                .findByCheckoutToken(checkoutToken)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Checkout not found"
                    )
                );

        if (!"PENDING".equals(payment.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Checkout is no longer available"
            );
        }

        if (payment.getCreatedAt().isBefore(
                OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(15)
        )) {
            throw new ResponseStatusException(
                HttpStatus.GONE,
                "Checkout expired"
            );
        }

        if (!"ACTIVE".equals(payment.getMerchant().getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Merchant is not active"
            );
        }

        return Map.of(
                "merchantName", payment.getMerchant().getBusinessName(),
                "amountPaise", payment.getAmountPaise(),
                "currency", payment.getCurrency(),
                "paymentId", payment.getPaymentId()
        );
    }

}
