
package com.nbd.payments.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class PasswordConfigTest {

    private final PasswordEncoder encoder =
            new PasswordConfig().passwordEncoder();

    @Test
    void shouldEncodePassword() {

        String password = "DemoPassword123!";

        String hash = encoder.encode(password);

        assertNotEquals(password, hash);
        assertTrue(encoder.matches(password, hash));
    }

    @Test
    void shouldRejectIncorrectPassword() {

        String hash = encoder.encode("DemoPassword123!");

        assertFalse(
            encoder.matches("WrongPassword123!", hash)
        );
    }

    @Test
    void shouldGenerateDifferentHashesForSamePassword() {

        String password = "DemoPassword123!";

        String hash1 = encoder.encode(password);
        String hash2 = encoder.encode(password);

        assertNotEquals(hash1, hash2);

        assertTrue(encoder.matches(password, hash1));
        assertTrue(encoder.matches(password, hash2));
    }
}
