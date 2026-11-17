
package com.nbd.payments.merchant.service;

import com.nbd.payments.email.service.EmailVerificationService;
import com.nbd.payments.merchant.entity.Merchant;
import com.nbd.payments.merchant.entity.MerchantCredential;
import com.nbd.payments.merchant.exception.DuplicateMerchantException;
import com.nbd.payments.merchant.repository.MerchantRepository;
import com.nbd.payments.merchant.repository.MerchantCredentialRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MerchantServiceTest {

    private MerchantRepository merchantRepository;
    private MerchantCredentialRepository credentialRepository;
    private PasswordEncoder passwordEncoder;
    private EmailVerificationService emailVerificationService;
    private MerchantService merchantService;

    private static final String EMAIL = "demo@example.test";
    private static final String PASSWORD = "DemoPassword123!";
    private static final String AUTH_TOKEN = "TEST_AUTH_TOKEN";

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
        void shouldRejectRegistrationWithoutAuthorization() {

        doThrow(new IllegalArgumentException(
                "Valid registration authorization is required"
        )).when(emailVerificationService)
                .consumeRegistrationAuthorization(EMAIL, null);

        assertThrows(
                IllegalArgumentException.class,
                () -> merchantService.registerMerchant(
                        "Demo Business",
                        EMAIL,
                        PASSWORD,
                        null
                )
        );

        verify(merchantRepository, never())
                .saveAndFlush(any(Merchant.class));

        verify(credentialRepository, never())
                .saveAndFlush(any(MerchantCredential.class));
        }

        @Test
        void shouldRejectRegistrationWithInvalidAuthorization() {

        doThrow(new IllegalArgumentException(
                "Registration authorization is invalid or expired"
        )).when(emailVerificationService)
                .consumeRegistrationAuthorization(EMAIL, AUTH_TOKEN);

        assertThrows(
                IllegalArgumentException.class,
                () -> merchantService.registerMerchant(
                        "Demo Business",
                        EMAIL,
                        PASSWORD,
                        AUTH_TOKEN
                )
        );

        verify(merchantRepository, never())
                .saveAndFlush(any(Merchant.class));

        verify(credentialRepository, never())
                .saveAndFlush(any(MerchantCredential.class));
        }

        @Test
        void shouldHandleDatabaseFailureDuringCredentialCreation() {

        doThrow(new DataIntegrityViolationException(
                "Synthetic database failure"
        )).when(credentialRepository)
                .saveAndFlush(any(MerchantCredential.class));

        assertThrows(
                DuplicateMerchantException.class,
                () -> merchantService.registerMerchant(
                        "Demo Business",
                        EMAIL,
                        PASSWORD,
                        AUTH_TOKEN
                )
        );

        verify(emailVerificationService)
                .consumeRegistrationAuthorization(
                        EMAIL,
                        AUTH_TOKEN
                );

        verify(merchantRepository)
                .saveAndFlush(any(Merchant.class));

        verify(credentialRepository)
                .saveAndFlush(any(MerchantCredential.class));
        }


        @Test
        void shouldRegisterVerifiedMerchantAndSaveCredentials() {

                Merchant merchant = merchantService.registerMerchant(
                        "Demo Business",
                        EMAIL,
                        PASSWORD,
                        AUTH_TOKEN
                );

                assertEquals("PENDING", merchant.getStatus());
                assertTrue(merchant.getMerchantId().startsWith("MER-"));
                assertTrue(merchant.isEmailVerified());

                verify(emailVerificationService)
                        .consumeRegistrationAuthorization(
                                EMAIL,
                                AUTH_TOKEN
                        );

                verify(passwordEncoder).encode(PASSWORD);

                verify(merchantRepository)
                        .saveAndFlush(any(Merchant.class));

                verify(credentialRepository)
                        .saveAndFlush(any(MerchantCredential.class));

                verify(emailVerificationService, never())
                        .sendVerificationEmail(anyString(), anyString());
        }

        @Test
        void shouldRejectDuplicateEmail() {

                when(merchantRepository.existsByEmailIgnoreCase(EMAIL))
                        .thenReturn(true);

                assertThrows(
                        DuplicateMerchantException.class,
                        () -> merchantService.registerMerchant(
                                "Demo Business",
                                EMAIL,
                                PASSWORD,
                                AUTH_TOKEN
                        )
                );

                verify(emailVerificationService, never())
                        .consumeRegistrationAuthorization(anyString(), anyString());

                verify(credentialRepository, never())
                        .saveAndFlush(any(MerchantCredential.class));
        }

        @Test
        void shouldRejectShortPassword() {

                assertThrows(
                        IllegalArgumentException.class,
                        () -> merchantService.registerMerchant(
                                "Demo Business",
                                EMAIL,
                                "123",
                                AUTH_TOKEN
                        )
                );

                verify(merchantRepository, never())
                        .saveAndFlush(any(Merchant.class));

                verify(emailVerificationService, never())
                        .consumeRegistrationAuthorization(anyString(), anyString());
        }

        @Test
        void shouldNormalizeEmail() {

                Merchant merchant = merchantService.registerMerchant(
                        "Demo Business",
                        "  DEMO@EXAMPLE.TEST  ",
                        PASSWORD,
                        AUTH_TOKEN
                );

                assertEquals(EMAIL, merchant.getEmail());

                verify(emailVerificationService)
                        .consumeRegistrationAuthorization(
                                EMAIL,
                                AUTH_TOKEN
                        );
        }

        @Test
        void shouldRejectLoginWhenEmailIsNotVerified() throws Exception {

                Merchant merchant = createMerchant(false, "ACTIVE");
                MerchantCredential credential = mockCredential(merchant);

                when(credentialRepository
                        .findByMerchant_EmailIgnoreCase(EMAIL))
                        .thenReturn(Optional.of(credential));

                when(passwordEncoder.matches(PASSWORD, "SYNTHETIC_HASH"))
                        .thenReturn(true);

                assertThrows(
                        DisabledException.class,
                        () -> merchantService.loginMerchant(EMAIL, PASSWORD)
                );
        }

        @Test
        void shouldRejectLoginWhenMerchantIsNotActive() throws Exception {

                Merchant merchant = createMerchant(true, "PENDING");
                MerchantCredential credential = mockCredential(merchant);

                when(credentialRepository
                        .findByMerchant_EmailIgnoreCase(EMAIL))
                        .thenReturn(Optional.of(credential));

                when(passwordEncoder.matches(PASSWORD, "SYNTHETIC_HASH"))
                        .thenReturn(true);

                assertThrows(
                        DisabledException.class,
                        () -> merchantService.loginMerchant(EMAIL, PASSWORD)
                );
        }

        @Test
        void shouldAllowLoginWhenEmailVerifiedAndMerchantActive() throws Exception {

                Merchant merchant = createMerchant(true, "ACTIVE");
                MerchantCredential credential = mockCredential(merchant);

                when(credentialRepository
                        .findByMerchant_EmailIgnoreCase(EMAIL))
                        .thenReturn(Optional.of(credential));

                when(passwordEncoder.matches(PASSWORD, "SYNTHETIC_HASH"))
                        .thenReturn(true);

                Merchant loggedInMerchant =
                        merchantService.loginMerchant(EMAIL, PASSWORD);

                assertEquals(EMAIL, loggedInMerchant.getEmail());
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
                        EMAIL
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
