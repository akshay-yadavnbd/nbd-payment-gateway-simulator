
package com.nbd.payments.merchant.repository;

import com.nbd.payments.merchant.entity.Merchant;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(
    replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE
)
class MerchantRepositoryTest {

    @Autowired
    private MerchantRepository merchantRepository;

    @Test
    void shouldSaveAndFindMerchantByMerchantId() {

        String uniqueValue = UUID.randomUUID()
                .toString()
                .replace("-", "");

        String merchantId = "MER-" + uniqueValue.substring(0, 12);

        String email = "merchant-" + uniqueValue
                + "@example.test";

        Merchant merchant = new Merchant(
                merchantId,
                "NBD Test Merchant",
                email
        );

        merchantRepository.saveAndFlush(merchant);

        Optional<Merchant> result =
                merchantRepository.findByMerchantId(merchantId);

        assertTrue(result.isPresent());

        assertEquals(
                "NBD Test Merchant",
                result.get().getBusinessName()
        );

        assertEquals(
                "PENDING",
                result.get().getStatus()
        );
    }

    @Test
    void shouldFindMerchantEmailIgnoringCase() {

        String uniqueValue = UUID.randomUUID()
                .toString()
                .replace("-", "");

        String merchantId = "MER-" + uniqueValue.substring(0, 12);

        String email = "merchant-" + uniqueValue
                + "@example.test";

        Merchant merchant = new Merchant(
                merchantId,
                "Email Test Merchant",
                email
        );

        merchantRepository.saveAndFlush(merchant);

        Optional<Merchant> result =
                merchantRepository.findByEmailIgnoreCase(
                        email.toUpperCase()
                );

        assertTrue(result.isPresent());
    }

    @Test
    void shouldReturnEmptyForUnknownMerchant() {

        Optional<Merchant> result =
                merchantRepository.findByMerchantId(
                        "MER-NOT-FOUND"
                );

        assertTrue(result.isEmpty());
    }
}
