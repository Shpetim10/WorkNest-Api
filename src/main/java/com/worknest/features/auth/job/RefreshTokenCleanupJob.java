package com.worknest.features.auth.job;

import com.worknest.features.auth.repository.RefreshTokenRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled job to clean up the refresh tokens table.
 * Removes tokens that are either expired or have been explicitly revoked.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Cleans up expired and revoked refresh tokens from the database.
     * Scheduled to run every day at 3:00 AM.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupTokens() {
        log.info("Starting scheduled cleanup of expired and revoked refresh tokens...");
        
        try {
            Instant now = Instant.now();
            refreshTokenRepository.deleteByExpiresAtBeforeOrRevokedAtIsNotNull(now);
            log.info("Successfully completed refresh token cleanup.");
        } catch (Exception e) {
            log.error("Failed to clean up refresh tokens", e);
        }
    }
}
