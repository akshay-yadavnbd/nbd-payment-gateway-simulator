
package com.nbd.payments.email.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
public class EmailVerificationService {

    private static final int TOKEN_EXPIRY_MINUTES = 30;

    private final JdbcTemplate jdbcTemplate;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    public EmailVerificationService(
            JdbcTemplate jdbcTemplate,
            EmailService emailService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.emailService = emailService;
    }

    @Transactional
    public void sendVerificationEmail(String email, String accountType) {

        if (!"MERCHANT".equals(accountType)
                && !"ADMIN".equals(accountType)) {
            throw new IllegalArgumentException("Invalid account type");
        }

        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);

        String tokenHash = hashToken(token);

        OffsetDateTime expiresAt =
                OffsetDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES);

        jdbcTemplate.update(
                """
                DELETE FROM email_verifications
                WHERE email = ?
                  AND account_type = ?
                  AND verified_at IS NULL
                """,
                email,
                accountType
        );

        jdbcTemplate.update(
                """
                INSERT INTO email_verifications
                    (email, account_type, token_hash, expires_at)
                VALUES (?, ?, ?, ?)
                """,
                email,
                accountType,
                tokenHash,
                Timestamp.from(expiresAt.toInstant())
        );

        String baseUrl = appBaseUrl.replaceAll("/+$", "");

        String verificationLink = baseUrl
                + "/api/v1/email-verification/verify?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);

        emailService.sendVerificationEmail(email, verificationLink);
    }

    @Transactional
    public void verifyEmail(String token) {

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Verification token is required");
        }

        String tokenHash = hashToken(token);

        List<VerificationRecord> records = jdbcTemplate.query(
                """
                SELECT email, account_type
                FROM email_verifications
                WHERE token_hash = ?
                  AND verified_at IS NULL
                  AND expires_at > CURRENT_TIMESTAMP
                """,
                (rs, rowNum) -> new VerificationRecord(
                        rs.getString("email"),
                        rs.getString("account_type")
                ),
                tokenHash
        );

        if (records.isEmpty()) {
            throw new IllegalArgumentException(
                    "Verification link is invalid, expired, or already used."
            );
        }

        VerificationRecord record = records.get(0);

        int updated = jdbcTemplate.update(
                """
                UPDATE email_verifications
                SET verified_at = CURRENT_TIMESTAMP
                WHERE token_hash = ?
                  AND verified_at IS NULL
                  AND expires_at > CURRENT_TIMESTAMP
                """,
                tokenHash
        );

        if (updated != 1) {
            throw new IllegalArgumentException(
                    "Verification link is invalid, expired, or already used."
            );
        }

        if ("MERCHANT".equals(record.accountType())) {

            int merchantUpdated = jdbcTemplate.update(
                    """
                    UPDATE merchants
                    SET email_verified = TRUE
                    WHERE LOWER(email) = LOWER(?)
                    """,
                    record.email()
            );

            if (merchantUpdated != 1) {
                throw new IllegalStateException(
                        "Merchant account was not found for email verification."
                );
            }
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

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

    private record VerificationRecord(
            String email,
            String accountType
    ) {}
}
