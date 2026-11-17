
package com.nbd.payments.email.controller;
import com.nbd.payments.email.service.EmailVerificationService;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/email-verification")
public class EmailVerificationController {

    private static final String AUTH_COOKIE =
            "NBD_REGISTRATION_AUTH";

    private static final Duration COOKIE_DURATION =
            Duration.ofHours(24);

    private final EmailVerificationService emailVerificationService;

    @Value("${app.security.cookie-secure:false}")
    private boolean secureCookie;

    public EmailVerificationController(
            EmailVerificationService emailVerificationService
    ) {
        this.emailVerificationService = emailVerificationService;
    }

    // Verify email and issue registration authorization

    @GetMapping("/verify")
    public ResponseEntity<Void> verifyEmail(
            @RequestParam(required = false) String token,
            HttpServletResponse response
    ) {

        try {

            EmailVerificationService.RegistrationAuthorization authorization =
                    emailVerificationService.verifyEmail(token);

            ResponseCookie cookie = ResponseCookie
                    .from(AUTH_COOKIE, authorization.token())
                    .httpOnly(true)
                    .secure(secureCookie)
                    .path("/")
                    .sameSite("Lax")
                    .maxAge(COOKIE_DURATION)
                    .build();

            response.addHeader(
                    HttpHeaders.SET_COOKIE,
                    cookie.toString()
            );

            return ResponseEntity
                    .status(HttpStatus.SEE_OTHER)
                    .location(URI.create("/merchant.html?verification=success"))
                    .build();

        } catch (IllegalArgumentException exception) {

            return ResponseEntity
                    .status(HttpStatus.SEE_OTHER)
                    .location(URI.create("/merchant.html?verification=invalid"))
                    .build();
        }
    }

    // Restore verification status after page refresh

    @GetMapping("/registration-status")
    public ResponseEntity<Map<String, Object>> registrationStatus(
            @CookieValue(
                    name = AUTH_COOKIE,
                    required = false
            ) String authorizationToken
    ) {

        Optional<String> email =
                emailVerificationService.isRegistrationAuthorized(
                        authorizationToken
                );

        if (email.isEmpty()) {

            return ResponseEntity.ok(
                    Map.of(
                            "verified", false,
                            "email", "",
                            "message", "Email verification required."
                    )
            );
        }

        return ResponseEntity.ok(
                Map.of(
                        "verified", true,
                        "email", email.get(),
                        "message", "Email verified successfully."
                )
        );
    }

    // Clear registration authorization

    @PostMapping("/clear-authorization")
    public ResponseEntity<Map<String, String>> clearAuthorization(
            HttpServletResponse response
    ) {

        ResponseCookie cookie = ResponseCookie
                .from(AUTH_COOKIE, "")
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );

        return ResponseEntity.ok(
                Map.of("message", "Registration authorization cleared.")
        );
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendVerificationEmail(
            @RequestBody Map<String, String> request
    ) {

        String email = request.get("email");

        emailVerificationService.sendVerificationEmail(
                email,
                "MERCHANT"
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Verification email sent successfully."
                )
        );
    }
}
