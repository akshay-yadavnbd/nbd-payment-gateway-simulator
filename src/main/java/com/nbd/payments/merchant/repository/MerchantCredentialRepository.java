package com.nbd.payments.merchant.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nbd.payments.merchant.entity.MerchantCredential;

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
}
