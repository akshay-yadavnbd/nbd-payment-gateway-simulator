
package com.nbd.payments.payment.repository;

import com.nbd.payments.payment.entity.Payment;
import com.nbd.payments.payment.entity.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentAttemptRepository
        extends JpaRepository<PaymentAttempt, Long> {

    List<PaymentAttempt> findByPaymentOrderByAttemptNumberAsc(
            Payment payment
    );

    Optional<PaymentAttempt> findByAttemptId(String attemptId);

    long countByPayment(Payment payment);
}