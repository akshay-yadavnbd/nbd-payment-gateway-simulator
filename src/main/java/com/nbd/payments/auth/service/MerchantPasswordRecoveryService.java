
package com.nbd.payments.auth.service;

import com.nbd.payments.auth.entity.PasswordResetToken;
import com.nbd.payments.auth.repository.PasswordResetTokenRepository;
import com.nbd.payments.email.service.EmailService;
import com.nbd.payments.merchant.entity.MerchantCredential;
import com.nbd.payments.merchant.repository.MerchantCredentialRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class MerchantPasswordRecoveryService {

    private static final int TOKEN_BYTES = 32;
    private static final int TOKEN_EXPIRY_MINUTES = 30;

    private final PasswordResetTokenRepository tokenRepository;
    private final MerchantCredentialRepository credentialRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.base-url}")
    private String appBaseUrl;

    public MerchantPasswordRecoveryService(
            PasswordResetTokenRepository tokenRepository,
            MerchantCredentialRepository credentialRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder
    ) {
        this.tokenRepository = tokenRepository;
        this.credentialRepository = credentialRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void requestPasswordReset(String email) {

        if (email == null || email.isBlank()) {
            return;
        }

        String normalizedEmail = email.trim()
                .toLowerCase(Locale.ROOT);

        MerchantCredential credential = credentialRepository
                    .findByMerchantEmailIgnoreCaseForUpdate(normalizedEmail)
                    .orElse(null);

        if (credential == null) {
            return;
        }

        String rawToken = generateToken();
        String tokenHash = hashToken(rawToken);

        Instant now = Instant.now();

        tokenRepository
                .findOutstandingByCredentialId(credential.getId())
                .forEach(PasswordResetToken::invalidate);

        PasswordResetToken resetToken = new PasswordResetToken(
                credential.getId(),
                tokenHash,
                now.plusSeconds(TOKEN_EXPIRY_MINUTES * 60L)
        );

        tokenRepository.save(resetToken);

        String baseUrl = appBaseUrl.replaceAll("/+$", "");

        String resetLink = baseUrl
                + "/merchant.html?resetToken="
                + java.net.URLEncoder.encode(
                        rawToken,
                        StandardCharsets.UTF_8
                );

        emailService.sendPasswordResetEmail(
                normalizedEmail,
                resetLink
        );
    }

    @Transactional
    public void resetPassword(
            String rawToken,
            String newPassword
    ) {

        validatePassword(newPassword);

        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Password reset link is invalid or expired."
            );
        }

        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken = tokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Password reset link is invalid or expired."
                ));

        if (!resetToken.isUsable(Instant.now())) {
            throw new IllegalArgumentException(
                    "Password reset link is invalid or expired."
            );
        }

        MerchantCredential credential = credentialRepository
                .findById(resetToken.getCredentialId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Password reset link is invalid or expired."
                ));

        credential.changePasswordHash(
                passwordEncoder.encode(newPassword)
        );

        resetToken.markUsed();
    }

    private String generateToken() {

        byte[] tokenBytes = new byte[TOKEN_BYTES];

        secureRandom.nextBytes(tokenBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);
    }

    private String hashToken(String token) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable",
                    exception
            );
        }
    }

    private void validatePassword(String password) {

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "Password is required."
            );
        }

        int byteLength = password.getBytes(StandardCharsets.UTF_8).length;

        if (password.length() < 8
                || password.length() > 72
                || byteLength > 72) {

            throw new IllegalArgumentException(
                    "Password must be between 8 and 72 characters."
            );
        }
    }
}
