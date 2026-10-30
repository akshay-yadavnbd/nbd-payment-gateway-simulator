
package com.nbd.payments.merchant.service;

import com.nbd.payments.merchant.entity.Merchant;
import com.nbd.payments.merchant.entity.MerchantCredential;
import com.nbd.payments.merchant.exception.DuplicateMerchantException;
import com.nbd.payments.merchant.repository.MerchantRepository;
import com.nbd.payments.merchant.repository.MerchantCredentialRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MerchantServiceTest {

    private MerchantRepository merchantRepository;
    private MerchantCredentialRepository credentialRepository;
    private PasswordEncoder passwordEncoder;
    private MerchantService merchantService;

    @BeforeEach
    void setUp() {

        merchantRepository = mock(MerchantRepository.class);
        credentialRepository = mock(MerchantCredentialRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);

        merchantService = new MerchantService(
                merchantRepository,
                credentialRepository,
                passwordEncoder
        );

        when(passwordEncoder.encode(anyString()))
                .thenReturn("SYNTHETIC_HASH");

        when(merchantRepository.saveAndFlush(any(Merchant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldRegisterMerchantAndSaveCredentials() {

        Merchant merchant = merchantService.registerMerchant(
                "Demo Business",
                "demo@example.test",
                "DemoPassword123!"
        );

        assertEquals("PENDING", merchant.getStatus());
        assertTrue(merchant.getMerchantId().startsWith("MER-"));

        verify(passwordEncoder).encode("DemoPassword123!");

        verify(merchantRepository)
                .saveAndFlush(any(Merchant.class));

        verify(credentialRepository)
                .saveAndFlush(any(MerchantCredential.class));
    }

    @Test
    void shouldRejectDuplicateEmail() {

        when(merchantRepository.existsByEmailIgnoreCase(
                "demo@example.test"
        )).thenReturn(true);

        assertThrows(
                DuplicateMerchantException.class,
                () -> merchantService.registerMerchant(
                        "Demo Business",
                        "demo@example.test",
                        "DemoPassword123!"
                )
        );

        verify(credentialRepository, never())
                .saveAndFlush(any(MerchantCredential.class));
    }

    @Test
    void shouldRejectShortPassword() {

        assertThrows(
                IllegalArgumentException.class,
                () -> merchantService.registerMerchant(
                        "Demo Business",
                        "demo@example.test",
                        "123"
                )
        );

        verify(merchantRepository, never())
                .saveAndFlush(any(Merchant.class));
    }

    @Test
    void shouldNormalizeEmail() {

        Merchant merchant = merchantService.registerMerchant(
                "Demo Business",
                "  DEMO@EXAMPLE.TEST  ",
                "DemoPassword123!"
        );

        assertEquals(
                "demo@example.test",
                merchant.getEmail()
        );
    }
}
