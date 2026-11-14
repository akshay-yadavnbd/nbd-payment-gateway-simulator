
package com.nbd.payments.merchant.service;

import com.nbd.payments.email.service.EmailVerificationService;
import com.nbd.payments.merchant.entity.Merchant;
import com.nbd.payments.merchant.entity.MerchantCredential;
import com.nbd.payments.merchant.exception.DuplicateMerchantException;
import com.nbd.payments.merchant.repository.MerchantRepository;
import com.nbd.payments.merchant.repository.MerchantCredentialRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MerchantServiceTest {

    private MerchantRepository merchantRepository;
    private MerchantCredentialRepository credentialRepository;
    private PasswordEncoder passwordEncoder;
    private EmailVerificationService emailVerificationService;
    private MerchantService merchantService;

    @BeforeEach
    void setUp() {

        merchantRepository = mock(MerchantRepository.class);
        credentialRepository = mock(MerchantCredentialRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        emailVerificationService = mock(EmailVerificationService.class);

        merchantService = new MerchantService(
                merchantRepository,
                credentialRepository,
                passwordEncoder,
                emailVerificationService
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
        assertFalse(merchant.isEmailVerified());

        verify(passwordEncoder).encode("DemoPassword123!");

        verify(merchantRepository)
                .saveAndFlush(any(Merchant.class));

        verify(credentialRepository)
                .saveAndFlush(any(MerchantCredential.class));

        verify(emailVerificationService)
                .sendVerificationEmail(
                        "demo@example.test",
                        "MERCHANT"
                );
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

        verify(emailVerificationService, never())
                .sendVerificationEmail(anyString(), anyString());
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

        verify(emailVerificationService, never())
                .sendVerificationEmail(anyString(), anyString());
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

        verify(emailVerificationService)
                .sendVerificationEmail(
                        "demo@example.test",
                        "MERCHANT"
                );
    }

    @Test
    void shouldRejectLoginWhenEmailIsNotVerified() throws Exception {

        Merchant merchant = createMerchant(false, "ACTIVE");
        MerchantCredential credential = mockCredential(merchant);

        when(credentialRepository
                .findByMerchant_EmailIgnoreCase("demo@example.test"))
                .thenReturn(Optional.of(credential));

        when(passwordEncoder.matches(
                "DemoPassword123!",
                "SYNTHETIC_HASH"
        )).thenReturn(true);

        assertThrows(
                DisabledException.class,
                () -> merchantService.loginMerchant(
                        "demo@example.test",
                        "DemoPassword123!"
                )
        );
    }

    @Test
    void shouldRejectLoginWhenMerchantIsNotActive() throws Exception {

        Merchant merchant = createMerchant(true, "PENDING");
        MerchantCredential credential = mockCredential(merchant);

        when(credentialRepository
                .findByMerchant_EmailIgnoreCase("demo@example.test"))
                .thenReturn(Optional.of(credential));

        when(passwordEncoder.matches(
                "DemoPassword123!",
                "SYNTHETIC_HASH"
        )).thenReturn(true);

        assertThrows(
                DisabledException.class,
                () -> merchantService.loginMerchant(
                        "demo@example.test",
                        "DemoPassword123!"
                )
        );
    }

    @Test
    void shouldAllowLoginWhenEmailVerifiedAndMerchantActive() throws Exception {

        Merchant merchant = createMerchant(true, "ACTIVE");
        MerchantCredential credential = mockCredential(merchant);

        when(credentialRepository
                .findByMerchant_EmailIgnoreCase("demo@example.test"))
                .thenReturn(Optional.of(credential));

        when(passwordEncoder.matches(
                "DemoPassword123!",
                "SYNTHETIC_HASH"
        )).thenReturn(true);

        Merchant loggedInMerchant = merchantService.loginMerchant(
                "demo@example.test",
                "DemoPassword123!"
        );

        assertEquals("demo@example.test", loggedInMerchant.getEmail());
        assertTrue(loggedInMerchant.isEmailVerified());
        assertEquals("ACTIVE", loggedInMerchant.getStatus());
    }

    private Merchant createMerchant(
            boolean emailVerified,
            String status
    ) throws Exception {

        Merchant merchant = new Merchant(
                "MER-TEST123",
                "Demo Business",
                "demo@example.test"
        );

        merchant.setStatus(status);

        Field field = Merchant.class.getDeclaredField("emailVerified");
        field.setAccessible(true);
        field.setBoolean(merchant, emailVerified);

        return merchant;
    }

    private MerchantCredential mockCredential(Merchant merchant) {

        MerchantCredential credential = mock(MerchantCredential.class);

        when(credential.getMerchant()).thenReturn(merchant);
        when(credential.getPasswordHash()).thenReturn("SYNTHETIC_HASH");

        return credential;
    }
}
