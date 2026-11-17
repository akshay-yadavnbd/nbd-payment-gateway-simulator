
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
import java.util.Locale;
import java.util.Optional;

@Service
public class EmailVerificationService {

       // private static final int TOKEN_EXPIRY_MINUTES = 30;

        private final JdbcTemplate jdbcTemplate;
        private final EmailService emailService;
        private final SecureRandom secureRandom = new SecureRandom();
        private static final int VERIFICATION_EXPIRY_MINUTES = 30;
        private static final int REGISTRATION_EXPIRY_HOURS = 24;

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
    public void sendVerificationEmail(
                        String email, 
                        String accountType
                ) {

                        if (!"MERCHANT".equals(accountType)
                                && !"ADMIN".equals(accountType)) {
                        throw new IllegalArgumentException(
                                        "Invalid account type"
                                );
                }
                if (email == null || email.isBlank()) {
                        throw new IllegalArgumentException(
                                "Email is required"
                        );
                }

                String normalizedEmail = email.trim()
                                .toLowerCase(Locale.ROOT);

                byte[] tokenBytes = new byte[32];
                secureRandom.nextBytes(tokenBytes);

                String token = Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(tokenBytes);

                String tokenHash = hashToken(token);

                OffsetDateTime expiresAt =
                        OffsetDateTime.now().plusMinutes(VERIFICATION_EXPIRY_MINUTES);

                
                jdbcTemplate.update(
                        """
                        UPDATE email_verifications
                        SET registration_consumed_at = CURRENT_TIMESTAMP
                        WHERE LOWER(email) = LOWER(?)
                        AND account_type = ?
                        AND registration_token_hash IS NOT NULL
                        AND registration_consumed_at IS NULL
                        """,
                        normalizedEmail,
                        accountType
                );

                jdbcTemplate.update(
                        """
                        DELETE FROM email_verifications
                        WHERE LOWER(email) = LOWER(?)
                                AND account_type = ?
                                AND verified_at IS NULL
                        """,
                        normalizedEmail,
                        accountType
        );

        jdbcTemplate.update(
                """
                INSERT INTO email_verifications
                    (email, account_type, token_hash, expires_at)
                VALUES (?, ?, ?, ?)
                """,
                normalizedEmail,
                accountType,
                tokenHash,
                Timestamp.from(expiresAt.toInstant())
        );

        String baseUrl = appBaseUrl.replaceAll("/+$", "");

        String verificationLink = baseUrl
                + "/api/v1/email-verification/verify?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);

        emailService.sendVerificationEmail(
                        normalizedEmail, 
                        verificationLink
                );
        }

        @Transactional
        public RegistrationAuthorization verifyEmail(String token) {

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                        "Verification token is required"
                );
        }

        String tokenHash = hashToken(token);

        List<VerificationRecord> records = jdbcTemplate.query(
                """
                SELECT email, account_type
                FROM email_verifications
                WHERE token_hash = ?
                  AND verified_at IS NULL
                  AND expires_at > CURRENT_TIMESTAMP
                FOR UPDATE
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

        byte[] authorizationBytes =new byte[32];
        secureRandom.nextBytes(authorizationBytes);

        String authorizationToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(authorizationBytes);

        String authorizationHash = hashToken(authorizationToken);

        OffsetDateTime authorizationExpiry = 
                        OffsetDateTime.now()
                                .plusHours(REGISTRATION_EXPIRY_HOURS);

        int updated = jdbcTemplate.update(
                """
                UPDATE email_verifications
                SET verified_at = CURRENT_TIMESTAMP,
                        registration_token_hash = ?,
                        registration_expires_at = ?,
                        registration_consumed_at = NULL
                WHERE token_hash = ?
                  AND verified_at IS NULL
                  AND expires_at > CURRENT_TIMESTAMP
                """,
                authorizationHash,
                Timestamp.from(authorizationExpiry.toInstant()),
                tokenHash
        );

        if (updated != 1) {
            throw new IllegalArgumentException(
                    "Verification link is invalid, expired, or already used."
            );
        }

        return  new RegistrationAuthorization(
                record.email(),
                record.accountType(),
                authorizationToken
        );
}
        @Transactional(readOnly = true)
        public Optional<String> isRegistrationAuthorized(
                String authorizationToken
        ){
                if (authorizationToken == null ||
                        authorizationToken.isBlank()
                ) {
                        return Optional.empty();
                }
                String authorizationHash = hashToken(authorizationToken);

                List<String> emails = jdbcTemplate.query(
                        """
                        SELECT email
                        FROM email_verifications
                        WHERE registration_token_hash =?
                                AND account_type = 'MERCHANT'
                                AND verified_at IS NOT NULL
                                AND registration_expires_at > CURRENT_TIMESTAMP
                                AND registration_consumed_at IS NULL
                                """,
                        (rs, rowNum) -> rs.getString("email"),
                        authorizationHash
                );

                return emails.stream().findFirst();
        }

        @Transactional
        public void consumeRegistrationAuthorization(
                String email,
                String authorizationToken
        ){
                if (email == null || email.isBlank() ||
                        authorizationToken == null ||
                        authorizationToken.isBlank()) {
                        throw new IllegalArgumentException(
                                "Valid registration authorization is required"
                        );
                }

                String normalizedEmail = email.trim()
                        .toLowerCase(Locale.ROOT);

                String authorizationHash = hashToken(authorizationToken);

                int updated = jdbcTemplate.update(
                        """
                        UPDATE email_verifications
                        SET registration_consumed_at = CURRENT_TIMESTAMP
                        WHERE LOWER(email) = LOWER(?)
                               AND account_type = 'MERCHANT'
                               AND registration_token_hash = ?
                               AND verified_at IS NOT NULL
                               AND registration_expires_at > CURRENT_TIMESTAMP
                               AND registration_consumed_at IS NULL
                        """,
                        normalizedEmail,
                        authorizationHash
                );

                if (updated !=1) {
                        throw new IllegalArgumentException(
                                "Registration authorization is invalid, expire or already used. Please verify your email again"
                        );
                }
        }

        private String hashToken(String token){
                try{
                        MessageDigest digest = 
                                MessageDigest.getInstance("SHA-256");

                        byte[] hash = digest.digest(
                                token.getBytes(StandardCharsets.UTF_8)
                        );

                        return HexFormat.of().formatHex(hash);
                } catch (NoSuchAlgorithmException exception){
                        throw new IllegalStateException(
                                "SHA-256 algorithm is unavailable",
                                exception
                        );
                }
        }

        public record RegistrationAuthorization(
                String email,
                String accountType,
                String token
        ) {}

        private record VerificationRecord(
                String email,
                String accountType
        ) {}
}