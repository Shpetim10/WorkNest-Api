package com.worknest.features.attendance.application;

import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.AttendanceQrChallenge;
import com.worknest.domain.entities.AttendanceQrTerminal;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.CompanySite;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.AttendanceQrChallengeStatus;
import com.worknest.domain.enums.AttendanceQrTerminalStatus;
import com.worknest.features.attendance.dto.CreateQrTerminalRequest;
import com.worknest.features.attendance.dto.QrCurrentTokenResponse;
import com.worknest.features.attendance.dto.QrTerminalSummaryDto;
import com.worknest.features.attendance.dto.QrValidateResponse;
import com.worknest.features.attendance.repository.AttendanceQrChallengeRepository;
import com.worknest.features.attendance.repository.AttendanceQrTerminalRepository;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.companySite.exception.SiteNotFoundException;
import com.worknest.features.companySite.repository.CompanySiteRepository;
import com.worknest.tenant.TenantContextHolder;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceQrService {

    private static final String DEFAULT_TERMINAL_NAME = "Default Terminal";

    private final CompanyRepository companyRepository;
    private final CompanySiteRepository companySiteRepository;
    private final AttendanceQrTerminalRepository attendanceQrTerminalRepository;
    private final AttendanceQrChallengeRepository attendanceQrChallengeRepository;
    private final AttendanceTokenService attendanceTokenService;
    private final AttendanceHashService attendanceHashService;

    public AttendanceQrTerminal ensureDefaultTerminal(Company company, CompanySite site) {
        return attendanceQrTerminalRepository
                .findFirstBySiteIdAndStatusOrderByCreatedAtAsc(site.getId(), AttendanceQrTerminalStatus.ACTIVE)
                .orElseGet(() -> {
                    AttendanceQrTerminal terminal = new AttendanceQrTerminal();
                    terminal.setCompany(company);
                    terminal.setSite(site);
                    terminal.setName(DEFAULT_TERMINAL_NAME);
                    terminal.setStatus(AttendanceQrTerminalStatus.ACTIVE);
                    terminal.setRotationSeconds(60);
                    terminal.setSecretKeyVersion("v1");
                    terminal.setAutoCreated(true);
                    return attendanceQrTerminalRepository.save(terminal);
                });
    }

    public QrTerminalSummaryDto createTerminal(UUID companyId, UUID siteId, CreateQrTerminalRequest request) {
        validateTenant(companyId);
        Company company = companyRepository.findById(companyId).orElseThrow(() -> new BusinessException(
                HttpStatus.NOT_FOUND,
                "COMPANY_NOT_FOUND",
                "Company does not exist."
        ));
        CompanySite site = companySiteRepository.findByIdAndCompanyId(siteId, companyId).orElseThrow(SiteNotFoundException::new);

        AttendanceQrTerminal terminal = new AttendanceQrTerminal();
        terminal.setCompany(company);
        terminal.setSite(site);
        terminal.setName(request.name().trim());
        terminal.setRotationSeconds(request.rotationSeconds() != null ? request.rotationSeconds() : 60);
        terminal.setStatus(AttendanceQrTerminalStatus.ACTIVE);
        terminal.setSecretKeyVersion("v1");
        terminal.setAutoCreated(false);
        AttendanceQrTerminal saved = attendanceQrTerminalRepository.save(terminal);
        return toSummary(saved);
    }

    @Transactional(readOnly = true)
    public List<QrTerminalSummaryDto> listSiteTerminals(UUID companyId, UUID siteId) {
        validateTenant(companyId);
        companySiteRepository.findByIdAndCompanyId(siteId, companyId).orElseThrow(SiteNotFoundException::new);
        return attendanceQrTerminalRepository.findAllBySiteIdOrderByCreatedAtAsc(siteId).stream().map(this::toSummary).toList();
    }

    public QrCurrentTokenResponse currentToken(UUID companyId, UUID terminalId) {
        validateTenant(companyId);
        AttendanceQrTerminal terminal = attendanceQrTerminalRepository.findByIdAndCompanyId(terminalId, companyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TERMINAL_NOT_FOUND", "QR terminal was not found."));
        if (terminal.getStatus() != AttendanceQrTerminalStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.CONFLICT, "TERMINAL_DISABLED", "QR terminal is not active.");
        }

        Instant now = Instant.now();
        AttendanceQrChallenge current = attendanceQrChallengeRepository
                .findFirstByQrTerminalIdAndStatusAndExpiresAtAfterOrderByExpiresAtDesc(
                        terminal.getId(),
                        AttendanceQrChallengeStatus.ACTIVE,
                        now.plusSeconds(5)
                )
                .orElseGet(() -> issueNewChallenge(terminal, now));

        return new QrCurrentTokenResponse(
                terminal.getId(),
                terminal.getSite().getId(),
                current.getNonce(),
                current.getIssuedAt(),
                current.getExpiresAt(),
                terminal.getRotationSeconds()
        );
    }

    public QrCurrentTokenResponse forceRefresh(UUID companyId, UUID terminalId) {
        validateTenant(companyId);
        AttendanceQrTerminal terminal = attendanceQrTerminalRepository.findByIdAndCompanyId(terminalId, companyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TERMINAL_NOT_FOUND", "QR terminal was not found."));
        if (terminal.getStatus() != AttendanceQrTerminalStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.CONFLICT, "TERMINAL_DISABLED", "QR terminal is not active.");
        }

        AttendanceQrChallenge issued = issueNewChallenge(terminal, Instant.now());
        return new QrCurrentTokenResponse(
                terminal.getId(),
                terminal.getSite().getId(),
                issued.getNonce(),
                issued.getIssuedAt(),
                issued.getExpiresAt(),
                terminal.getRotationSeconds()
        );
    }

    public AttendanceQrChallenge validateAndConsumeToken(String rawToken, UUID companyId, UUID siteId, User usedBy) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "QR_REQUIRED", "QR token is required.");
        }

        String tokenHash = attendanceHashService.sha256(rawToken.trim());
        AttendanceQrChallenge challenge = attendanceQrChallengeRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "QR_INVALID", "QR token is invalid."));

        if (!challenge.getCompany().getId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "QR_COMPANY_MISMATCH", "QR token belongs to a different company.");
        }
        if (!challenge.getSite().getId().equals(siteId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "QR_SITE_MISMATCH", "QR token belongs to a different site.");
        }
        if (challenge.getStatus() == AttendanceQrChallengeStatus.USED || challenge.getUsedAt() != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "QR_REPLAYED", "QR token has already been used.");
        }
        if (challenge.getExpiresAt().isBefore(Instant.now())) {
            challenge.setStatus(AttendanceQrChallengeStatus.EXPIRED);
            attendanceQrChallengeRepository.save(challenge);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "QR_EXPIRED", "QR token is expired.");
        }

        challenge.setStatus(AttendanceQrChallengeStatus.USED);
        challenge.setUsedAt(Instant.now());
        challenge.setUsedBy(usedBy);
        AttendanceQrChallenge saved = attendanceQrChallengeRepository.save(challenge);

        // Ensure the display terminal can immediately expose a fresh token after successful scan.
        issueNewChallenge(saved.getQrTerminal(), Instant.now());
        return saved;
    }

    @Transactional(readOnly = true)
    public QrValidateResponse validateToken(UUID companyId, UUID siteId, String rawToken) {
        validateTenant(companyId);
        if (rawToken == null || rawToken.isBlank()) {
            return new QrValidateResponse(false, "QR_REQUIRED", "QR token is required.", null, null);
        }

        String tokenHash = attendanceHashService.sha256(rawToken.trim());
        AttendanceQrChallenge challenge = attendanceQrChallengeRepository.findByTokenHash(tokenHash).orElse(null);
        if (challenge == null) {
            return new QrValidateResponse(false, "QR_INVALID", "QR token is invalid.", null, null);
        }
        if (!challenge.getCompany().getId().equals(companyId)) {
            return new QrValidateResponse(false, "QR_COMPANY_MISMATCH", "QR token belongs to another company.", challenge.getQrTerminal().getId(), challenge.getExpiresAt());
        }
        if (!challenge.getSite().getId().equals(siteId)) {
            return new QrValidateResponse(false, "QR_SITE_MISMATCH", "QR token belongs to another site.", challenge.getQrTerminal().getId(), challenge.getExpiresAt());
        }
        if (challenge.getStatus() == AttendanceQrChallengeStatus.USED || challenge.getUsedAt() != null) {
            return new QrValidateResponse(false, "QR_REPLAYED", "QR token has already been used.", challenge.getQrTerminal().getId(), challenge.getExpiresAt());
        }
        if (challenge.getExpiresAt().isBefore(Instant.now())) {
            return new QrValidateResponse(false, "QR_EXPIRED", "QR token is expired.", challenge.getQrTerminal().getId(), challenge.getExpiresAt());
        }
        return new QrValidateResponse(true, "VALID", "QR token is valid.", challenge.getQrTerminal().getId(), challenge.getExpiresAt());
    }

    private AttendanceQrChallenge issueNewChallenge(AttendanceQrTerminal terminal, Instant now) {
        String rawToken = attendanceTokenService.generateRawToken();
        AttendanceQrChallenge challenge = new AttendanceQrChallenge();
        challenge.setCompany(terminal.getCompany());
        challenge.setSite(terminal.getSite());
        challenge.setQrTerminal(terminal);
        challenge.setNonce(rawToken);
        challenge.setTokenHash(attendanceHashService.sha256(rawToken));
        challenge.setIssuedAt(now);
        challenge.setExpiresAt(now.plusSeconds(terminal.getRotationSeconds()));
        challenge.setStatus(AttendanceQrChallengeStatus.ACTIVE);
        return attendanceQrChallengeRepository.save(challenge);
    }

    private void validateTenant(UUID companyId) {
        UUID tenantCompanyId = TenantContextHolder.get()
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "TENANT_CONTEXT_MISSING", "No tenant context found."))
                .companyId();
        if (!tenantCompanyId.equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Cross-tenant access is not allowed.");
        }
    }

    private QrTerminalSummaryDto toSummary(AttendanceQrTerminal terminal) {
        return new QrTerminalSummaryDto(
                terminal.getId(),
                terminal.getName(),
                terminal.getStatus(),
                terminal.getRotationSeconds(),
                Boolean.TRUE.equals(terminal.getAutoCreated()),
                terminal.getLastHeartbeatAt()
        );
    }
}
