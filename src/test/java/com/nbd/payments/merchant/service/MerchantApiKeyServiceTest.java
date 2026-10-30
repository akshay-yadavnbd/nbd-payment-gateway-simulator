
package com.nbd.payments.merchant.service;

import com.nbd.payments.merchant.entity.Merchant;
import com.nbd.payments.merchant.entity.MerchantApiKey;
import com.nbd.payments.merchant.repository.MerchantRepository;
import com.nbd.payments.merchant.repository.MerchantApiKeyRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MerchantApiKeyServiceTest {

    private MerchantRepository merchantRepository;
    private MerchantApiKeyRepository apiKeyRepository;
    private MerchantApiKeyService apiKeyService;

    @BeforeEach
    void setUp() {

        merchantRepository = mock(MerchantRepository.class);
        apiKeyRepository = mock(MerchantApiKeyRepository.class);

        apiKeyService = new MerchantApiKeyService(
                merchantRepository,
                apiKeyRepository
        );
    }

    @Test
    void shouldGenerateAndHashApiKey() {

        Merchant merchant = new Merchant(
                "MER-TEST001",
                "Demo Merchant",
                "demo@example.test"
        );

        merchant.setStatus("ACTIVE");

        when(merchantRepository.findByMerchantId("MER-TEST001"))
                .thenReturn(Optional.of(merchant));

        when(apiKeyRepository.saveAndFlush(any(MerchantApiKey.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String apiKey = apiKeyService.generateApiKey("MER-TEST001");

        assertTrue(apiKey.startsWith("nbd_test_"));
        assertEquals(73, apiKey.length());

        verify(apiKeyRepository)
                .saveAndFlush(any(MerchantApiKey.class));
    }

    @Test
    void shouldRejectInactiveMerchant() {

        Merchant merchant = new Merchant(
                "MER-TEST002",
                "Pending Merchant",
                "pending@example.test"
        );

        when(merchantRepository.findByMerchantId("MER-TEST002"))
                .thenReturn(Optional.of(merchant));

        assertThrows(
                IllegalStateException.class,
                () -> apiKeyService.generateApiKey("MER-TEST002")
        );

        verify(apiKeyRepository, never())
                .saveAndFlush(any(MerchantApiKey.class));
    }

    @Test
    void shouldGenerateConsistentHash() {

        String apiKey = "nbd_test_example";

        String hash1 = apiKeyService.hashApiKey(apiKey);
        String hash2 = apiKeyService.hashApiKey(apiKey);

        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length());
    }
}
