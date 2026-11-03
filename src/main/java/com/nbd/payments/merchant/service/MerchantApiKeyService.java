
package com.nbd.payments.merchant.service;

import com.nbd.payments.merchant.entity.Merchant;
import com.nbd.payments.merchant.entity.MerchantApiKey;
import com.nbd.payments.merchant.repository.MerchantRepository;
import com.nbd.payments.merchant.repository.MerchantApiKeyRepository;

import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

@Service
public class MerchantApiKeyService {

    private final MerchantRepository merchantRepository;
    private final MerchantApiKeyRepository apiKeyRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    public MerchantApiKeyService(
            MerchantRepository merchantRepository,
            MerchantApiKeyRepository apiKeyRepository
    ) {
        this.merchantRepository = merchantRepository;
        this.apiKeyRepository = apiKeyRepository;
    }

    @Transactional
    public String generateApiKey(String merchantId) {

        Merchant merchant = merchantRepository
                .findByMerchantId(merchantId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Merchant not found"
                        )
                );

        if (!"ACTIVE".equals(merchant.getStatus())) {
            throw new DisabledException(
                    "Merchant must be ACTIVE"
            );
        }

        if (apiKeyRepository.existsByMerchant_Id(
                merchant.getId()
        )) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "API key has been already generated, enter your API key"
            );
        }

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String apiKey = "nbd_test_" +
                HexFormat.of().formatHex(randomBytes);

        String keyHash = hashApiKey(apiKey);

        MerchantApiKey entity = new MerchantApiKey(
                merchant,
                keyHash
        );

        apiKeyRepository.saveAndFlush(entity);

        return apiKey;
    }

    public String hashApiKey(String apiKey) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    apiKey.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "SHA-256 algorithm unavailable",
                    ex
            );
        }
    }

    @Transactional(readOnly = true)
    public Merchant authenticateApiKey(String apiKey){
        if (
                apiKey == null || 
                apiKey.isBlank() ||
                !apiKey.matches("nbd_test_[0-9a-f]{64}")
        ) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                "Invalid Api Key"
            );
        }

        String keyHash = hashApiKey(apiKey);

        MerchantApiKey storedKey = apiKeyRepository
                .findByKeyHash(keyHash)
                .orElseThrow(() ->
                    new org.springframework.security.authentication.BadCredentialsException(
                        "Invalid Api Key"
                    )
            );

        Merchant merchant = storedKey.getMerchant();
        
        if (!"ACTIVE".equals(merchant.getStatus())) {
            throw new org.springframework.security.authentication.DisabledException(
                "Merchant account is not active"
            );
            
        }
        return  merchant;
    }
}
