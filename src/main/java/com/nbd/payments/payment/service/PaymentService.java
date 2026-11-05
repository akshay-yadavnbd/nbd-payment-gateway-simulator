
package com.nbd.payments.payment.service;

import com.nbd.payments.merchant.entity.Merchant;
import com.nbd.payments.payment.dto.CardPaymentRequest;
import com.nbd.payments.payment.dto.PaymentResponse;
import com.nbd.payments.payment.entity.Payment;
import com.nbd.payments.payment.repository.PaymentRepository;
import com.nbd.payments.payment.repository.PaymentAttemptRepository;
import com.nbd.payments.payment.entity.PaymentAttempt;
import com.nbd.payments.payment.dto.CardPaymentResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;

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
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final TransactionTemplate transactionTemplate;


    
        public PaymentService(
                PaymentRepository paymentRepository,
                PaymentScenarioSimulator simulator,
                PaymentAttemptRepository paymentAttemptRepository,
                TransactionTemplate transactionTemplate
        ) {
                this.paymentRepository = paymentRepository;
                this.simulator = simulator;
                this.paymentAttemptRepository = paymentAttemptRepository;
                this.transactionTemplate = transactionTemplate;

                this.transactionTemplate.setPropagationBehavior(
                        org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW
                );
        }

        private String generateAttemptId(){
                return "ATT-" +
                        UUID.randomUUID()
                                .toString()
                                .replace("_", "")
                                .toUpperCase();
        }


        //@Transactional
        public PaymentResponse createPayment(
                Merchant merchant,
                Long amountPaise,
                String idempotencyKey
        ) {

                if (merchant == null || !"ACTIVE".equals(merchant.getStatus())) {
                throw new IllegalArgumentException(
                        "Merchant is not active"
                );
                }

                if (amountPaise == null || amountPaise < 100L) {
                throw new IllegalArgumentException(
                        "Enter a valid payment amount, minimum amount is ₹1.00"
                );
                }

                if (idempotencyKey == null ||
                        idempotencyKey.isBlank() ||
                        idempotencyKey.length()>100
                ) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Valid Idempotency-key header is required"
                        );
                }

                var existingPayment = paymentRepository
                                .findByMerchant_IdAndIdempotencyKey(
                                        merchant.getId(), 
                                        idempotencyKey
                                );
                if (existingPayment.isPresent()) {
                        Payment payment = existingPayment.get();

                        if (!payment.getAmountPaise().equals(amountPaise) ||
                                !"INR".equals(payment.getCurrency())) {
                                
                                        throw new ResponseStatusException(
                                                HttpStatus.CONFLICT,
                                                "Idempotency key already used with different payment details"
                                        );
                        }
                        return PaymentResponse.from(payment);
                }

                //generating unique payment ID
                String paymentId = "PAY-" +
                        UUID.randomUUID()
                                .toString()
                                .replace("-", "")
                                .toUpperCase();

                //generating secure checkout token 
                byte[] randomBytes = new byte[32];
                secureRandom.nextBytes(randomBytes);

                String checkoutToken = HexFormat.of()
                        .formatHex(randomBytes);

                try{
                        return transactionTemplate.execute(status -> {
                                ////payment creation
                                Payment payment = new Payment(
                                paymentId,
                                merchant,
                                checkoutToken,
                                amountPaise
                        );

                        payment.setIdempotencyKey(idempotencyKey);
                        Payment savedPayment = 
                                        paymentRepository.saveAndFlush(payment);
                        return PaymentResponse.from(savedPayment);
                        });
                }catch (DataIntegrityViolationException exception){
                        return findExistingPayment(
                                                merchant.getId(), 
                                                idempotencyKey, 
                                                amountPaise
                                        );
                }

        }

        //Updated this method from PaymentResponse to CardPaymentResponse

        @Transactional
        public CardPaymentResponse processCardPayment(
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
                        "Payment is no longer available"
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

        long existingAttempts =
                paymentAttemptRepository.countByPayment(payment);

        if (existingAttempts >= 5) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Maximum payment attempts reached"
                );
        }

        short attemptNumber = (short) (existingAttempts + 1);

        String result = simulator.evaluate(
                request.cardNumber(),
                request.expiry(),
                request.cvv()
        );

        String attemptStatus =
                "SUCCESS".equals(result)
                        ? "SUCCESS"
                        : "AUTHENTICATION_FAILED".equals(result)
                                ? "AUTHENTICATION_FAILED"
                                : "FAILED";

        String errorCode =
                "SUCCESS".equals(result) ? null : result;

        PaymentAttempt attempt = new PaymentAttempt(
                generateAttemptId(),
                payment,
                attemptNumber,
                payment.getAmountPaise(),
                attemptStatus,
                errorCode
        );

        paymentAttemptRepository.save(attempt);

        if ("SUCCESS".equals(result)) {

                payment.markSuccess();

        } else if (attemptNumber == 5) {

                payment.markFailed(result);

        } else {

                payment.recordFailedAttempt(result);
        }

        paymentRepository.saveAndFlush(payment);

        paymentAttemptRepository.flush();

        String message = simulator.messageFor(result);

        if (!"SUCCESS".equals(result) && attemptNumber == 5) {
                message += ". Maximum attempts reached. Please request a new payment link.";
        }

        return new CardPaymentResponse(
                payment.getPaymentId(),
                attempt.getAttemptId(),
                payment.getStatus(),
                attempt.getStatus(),
                errorCode,
                message,
                attemptNumber,
                5 - attemptNumber
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

                long attemptsUsed =
                        paymentAttemptRepository.countByPayment(payment);
                        
                return Map.of(
                        "merchantName", payment.getMerchant().getBusinessName(),
                        "amountPaise", payment.getAmountPaise(),
                        "currency", payment.getCurrency(),
                        "paymentId", payment.getPaymentId(),
                        "attemptsUsed", attemptsUsed,
                        "attemptsRemaining", Math.max(0, 5-attemptsUsed)
                );
        }

        private  PaymentResponse findExistingPayment(
                Long merchantId,
                String idempotencyKey,
                Long amountPaise
        ){
                Payment payment = paymentRepository
                        .findByMerchant_IdAndIdempotencyKey(
                                merchantId,
                                idempotencyKey
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Payment creation conflict. Retry using the same Idempotency-key"
                                )        
                );

                if (!payment.getAmountPaise().equals(amountPaise) ||
                        !"INR".equals(payment.getCurrency())) {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Idempotency key already used with different payment details"
                        );
                }
                return PaymentResponse.from(payment);
        }

}
