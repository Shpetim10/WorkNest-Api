package com.worknest.features.attendance.repository;

import com.worknest.domain.entities.AttendanceQrChallenge;
import com.worknest.domain.enums.AttendanceQrChallengeStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceQrChallengeRepository extends JpaRepository<AttendanceQrChallenge, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c
            from AttendanceQrChallenge c
            where c.tokenHash = :tokenHash
            """)
    Optional<AttendanceQrChallenge> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    Optional<AttendanceQrChallenge> findByTokenHash(String tokenHash);

    Optional<AttendanceQrChallenge> findFirstByQrTerminalIdAndStatusAndExpiresAtAfterOrderByExpiresAtDesc(
            UUID qrTerminalId,
            AttendanceQrChallengeStatus status,
            Instant now
    );
}
