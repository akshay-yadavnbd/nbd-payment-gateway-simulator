
package com.nbd.payments.merchant.repository;

import com.nbd.payments.merchant.entity.Merchant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantRepository
        extends JpaRepository<Merchant, Long> {

    Optional<Merchant> findByMerchantId(String merchantId);

    Optional<Merchant> findByEmailIgnoreCase(String email);

    boolean existsByMerchantId(String merchantId);

    boolean existsByEmailIgnoreCase(String email);

}
