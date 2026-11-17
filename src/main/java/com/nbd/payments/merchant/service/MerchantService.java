package com.nbd.payments.merchant.service;

import com.nbd.payments.email.service.EmailVerificationService;
import com.nbd.payments.merchant.entity.Merchant;
import com.nbd.payments.merchant.entity.MerchantCredential;
import com.nbd.payments.merchant.repository.MerchantCredentialRepository;
import com.nbd.payments.merchant.repository.MerchantRepository;
import com.nbd.payments.merchant.exception.DuplicateMerchantException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

@Service
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final MerchantCredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    public MerchantService(
            MerchantRepository merchantRepository,
            MerchantCredentialRepository credentialRepository,
            PasswordEncoder passwordEncoder,
            EmailVerificationService emailVerificationService
        ) {
        this.merchantRepository = merchantRepository;
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
    }

    //Merchant registration

    @Transactional
    public Merchant registerMerchant(
            String businessName,
            String email,
            String password,
            String authorizationToken
    ) {

        if (businessName == null || businessName.isBlank()) {
            throw new IllegalArgumentException(
                "Business name is required"
            );
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                "Email is required"
            );
        }

        String normalizedBusinessName = businessName.trim();

        if (normalizedBusinessName.length() > 150) {
            throw new IllegalArgumentException(
                "Business name is too long"
            );
        }

        String normalizedEmail = email.trim()
                .toLowerCase(Locale.ROOT);

        if (normalizedEmail.length() > 254) {
            throw new IllegalArgumentException(
                "Email is too long"
            );
        }

        if (!normalizedEmail.matches(
                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
        )) {
            throw new IllegalArgumentException(
                "Invalid email address"
            );
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                "Password is required"
            );
        }

        if (password.length()<8 || 
                password.length()>72 ||
                password.getBytes(StandardCharsets.UTF_8).length>72) {
            throw new IllegalArgumentException(
                "Password must be between 8 and 72 characters "
            );
        }

        if (merchantRepository.existsByEmailIgnoreCase(normalizedEmail)) {

                throw new DuplicateMerchantException(
                    "Merchant email already registered"
                );
        }

        emailVerificationService.consumeRegistrationAuthorization(
            normalizedEmail,
            authorizationToken
        );

        String merchantId = "MER-" +
                UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .toUpperCase(Locale.ROOT);

        Merchant merchant = new Merchant(
                merchantId,
                normalizedBusinessName,
                normalizedEmail
        );

        String passwordHash = passwordEncoder.encode(password);

        try {
            merchant.setEmailVerified(true);
            Merchant savedMerchant = 
                    merchantRepository.saveAndFlush(merchant);

            MerchantCredential credential = 
            new MerchantCredential(
                savedMerchant, 
                passwordHash
            );

            credentialRepository.saveAndFlush(credential);

            return savedMerchant;

        } catch (DataIntegrityViolationException exception) {
           
            throw new DuplicateMerchantException(
                "Merchant registration conflicts with an existing record"
            );
        }
    }
        
    // Merchant Login

    @Transactional(readOnly = true)
    public Merchant loginMerchant(
            String email,
            String password
    ) {
        if (
            email == null || 
                email.isBlank() || 
                password == null || 
                password.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Your Email and password are required"
            );
        }

        String normalizedEmail = email.trim()
                .toLowerCase(Locale.ROOT);

        MerchantCredential credential =
                credentialRepository
                    .findByMerchant_EmailIgnoreCase(normalizedEmail)
                    .orElseThrow(() ->
                        new BadCredentialsException(
                            "Invalid email or password"
                        )
                    );

        if (!passwordEncoder.matches(
                password,
                credential.getPasswordHash()
        )) {
            throw new BadCredentialsException(
                "Invalid email or password"
            );
        }

        Merchant merchant = credential.getMerchant();

        if (!merchant.isEmailVerified()) {
            throw new DisabledException(
                "Email not verified. Please verify your email before logging in."
            );
        }

        if (!"ACTIVE".equals(merchant.getStatus())) {
            throw new DisabledException(
                "Account is not active"
            );
        }

        return merchant;
    }
    
    @Transactional(readOnly = true)
    public Merchant getActiveMerchant(String merchantId) {

        Merchant merchant = merchantRepository
                .findByMerchantId(merchantId)
                .orElseThrow(() ->
                        new BadCredentialsException(
                                        "Merchant not found"
                                )
                );

        if (!"ACTIVE".equals(merchant.getStatus())) {
            throw new org.springframework.security.authentication
                    .DisabledException(
                            "Merchant account is not active"
                    );
        }

        return merchant;
    }
}
