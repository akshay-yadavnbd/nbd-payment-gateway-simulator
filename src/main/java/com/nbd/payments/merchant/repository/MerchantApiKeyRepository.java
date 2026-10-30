
package com.nbd.payments.merchant.repository;

import com.nbd.payments.merchant.entity.MerchantApiKey;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantApiKeyRepository
        extends JpaRepository<MerchantApiKey, Long> {

    Optional<MerchantApiKey> findByKeyHash(String keyHash);

    boolean existsByMerchant_Id(Long merchantId);
}
