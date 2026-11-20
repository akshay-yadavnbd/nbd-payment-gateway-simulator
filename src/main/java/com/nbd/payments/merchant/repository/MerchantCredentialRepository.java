package com.nbd.payments.merchant.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nbd.payments.merchant.entity.MerchantCredential;

import jakarta.persistence.LockModeType;

@Repository 
public interface MerchantCredentialRepository extends JpaRepository<MerchantCredential, Long>{
    Optional<MerchantCredential> findByMerchant_Id(Long merchantId);

    Optional<MerchantCredential> findByMerchant_MerchantId(
        String merchantId
    );

    Optional<MerchantCredential> findByMerchant_EmailIgnoreCase(
        String email
    );

    boolean existsByMerchant_Id(Long merchantId);  
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c FROM MerchantCredential c
        WHERE LOWER(c.merchant.email) = LOWER(:email)
    """)
    Optional<MerchantCredential> findByMerchantEmailIgnoreCaseForUpdate(
            @Param("email") String email
    );
}
