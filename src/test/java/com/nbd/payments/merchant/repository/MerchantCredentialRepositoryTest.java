
package com.nbd.payments.merchant.repository;

import com.nbd.payments.merchant.entity.Merchant;
import com.nbd.payments.merchant.entity.MerchantCredential;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE
)
class MerchantCredentialRepositoryTest {

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private MerchantCredentialRepository credentialRepository;

    @Test
    void shouldSaveAndFindCredentialByPublicMerchantId() {

        String uniqueId = UUID.randomUUID()
                .toString()
                .replace("-", "");

        String merchantId = "MER-" + uniqueId.substring(0, 12);

        Merchant merchant = new Merchant(
                merchantId,
                "Credential Test Merchant",
                "credential-" + uniqueId + "@example.test"
        );

        merchant = merchantRepository.saveAndFlush(merchant);

        MerchantCredential credential = new MerchantCredential(
                merchant,
                "TEST_ONLY_NOT_A_REAL_PASSWORD_HASH"
        );

        credentialRepository.saveAndFlush(credential);

        Optional<MerchantCredential> result =
                credentialRepository.findByMerchant_MerchantId(
                        merchantId
                );

        assertTrue(result.isPresent());

        assertEquals(
                merchantId,
                result.get().getMerchant().getMerchantId()
        );

        assertEquals(
                0,
                result.get().getFailedLoginAttempts()
        );

        assertNotNull(result.get().getCreatedAt());
    }

    @Test
    void shouldFindCredentialByMerchantEmailIgnoringCase() {

        String uniqueId = UUID.randomUUID()
                .toString()
                .replace("-", "");

        Merchant merchant = new Merchant(
                "MER-" + uniqueId.substring(0, 12),
                "Email Lookup Merchant",
                "login-" + uniqueId + "@example.test"
        );

        merchant = merchantRepository.saveAndFlush(merchant);

        MerchantCredential credential = new MerchantCredential(
                merchant,
                "TEST_ONLY_NOT_A_REAL_PASSWORD_HASH"
        );

        credentialRepository.saveAndFlush(credential);

        Optional<MerchantCredential> result =
                credentialRepository.findByMerchant_EmailIgnoreCase(
                        merchant.getEmail().toUpperCase()
                );

        assertTrue(result.isPresent());

        assertEquals(
                merchant.getId(),
                result.get().getMerchant().getId()
        );
    }

    @Test
    void shouldReturnEmptyForUnknownMerchant() {

        Optional<MerchantCredential> result =
                credentialRepository.findByMerchant_MerchantId(
                        "MER-NONEXISTENT"
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindCredentialByInternalMerchantId() {

        String uniqueId = UUID.randomUUID()
                .toString()
                .replace("-", "");

        Merchant merchant = new Merchant(
                "MER-" + uniqueId.substring(0, 12),
                "Internal Lookup Merchant",
                "internal-" + uniqueId + "@example.test"
        );

        merchant = merchantRepository.saveAndFlush(merchant);

        credentialRepository.saveAndFlush(
                new MerchantCredential(
                        merchant,
                        "TEST_ONLY_NOT_A_REAL_PASSWORD_HASH"
                )
        );

        Optional<MerchantCredential> result =
                credentialRepository.findByMerchant_Id(
                        merchant.getId()
                );

        assertTrue(result.isPresent());

        assertTrue(
                credentialRepository.existsByMerchant_Id(
                        merchant.getId()
                )
        );
    }
}
