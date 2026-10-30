
package com.nbd.payments.payment.repository;

import com.nbd.payments.payment.entity.Payment;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentId(String paymentId);

    Optional<Payment> findByCheckoutToken(String checkoutToken);

        
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.checkoutToken = :token")
    Optional<Payment> findWithLockByCheckoutToken(
            @Param("token") String token
    );

    List<Payment> findByMerchant_IdOrderByCreatedAtDesc(Long merchantId);

    List<Payment> findAllByOrderByCreatedAtDesc();

    @Query("""
        SELECT COALESCE(SUM(p.settledAmountPaise), 0)
        FROM Payment p
        WHERE p.merchant.id = :merchantId
        """)
    Long calculateMerchantBalance(
            @Param("merchantId") Long merchantId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p FROM Payment p
        WHERE p.paymentId = :paymentId
        AND p.merchant.id = :merchantId
        """)
    Optional<Payment> findForRefund(
            @Param("paymentId") String paymentId,
            @Param("merchantId") Long merchantId
    );

}

