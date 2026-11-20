
package com.nbd.payments.auth.repository;

import com.nbd.payments.auth.entity.PasswordResetToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT t
        FROM PasswordResetToken t
        WHERE t.tokenHash = :tokenHash
    """)
    Optional<PasswordResetToken> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    @Query("""
        SELECT t
        FROM PasswordResetToken t
        WHERE t.credentialId = :credentialId
          AND t.usedAt IS NULL
          AND t.invalidatedAt IS NULL
    """)
    java.util.List<PasswordResetToken> findOutstandingByCredentialId(
            @Param("credentialId") Long credentialId
    );
}
