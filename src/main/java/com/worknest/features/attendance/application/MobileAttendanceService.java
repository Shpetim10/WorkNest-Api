package com.worknest.features.attendance.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worknest.common.exception.BusinessException;
import com.worknest.common.web.ClientIpResolver;
import com.worknest.domain.entities.AttendanceDayRecord;
import com.worknest.domain.entities.AttendanceEvent;
import com.worknest.domain.entities.AttendancePolicy;
import com.worknest.domain.entities.AttendanceQrChallenge;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.CompanySite;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.SiteTrustedNetwork;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.CompanyStatus;
import com.worknest.domain.enums.AttendanceCaptureMethod;
import com.worknest.domain.enums.AttendanceDayStatus;
import com.worknest.domain.enums.AttendanceDecision;
import com.worknest.domain.enums.AttendanceEventStatus;
import com.worknest.domain.enums.AttendanceEventType;
import com.worknest.domain.enums.AttendanceReviewStatus;
import com.worknest.domain.enums.AttendanceState;
import com.worknest.domain.enums.AttendanceWarningCode;
import com.worknest.domain.enums.EmploymentStatus;
import com.worknest.domain.enums.GeofenceDecision;
import com.worknest.domain.enums.NextAttendanceAction;
import com.worknest.domain.enums.NetworkDecision;
import com.worknest.domain.enums.SiteStatus;
import com.worknest.features.attendance.dto.AttendanceRecordDto;
import com.worknest.features.attendance.dto.AttendanceWarningDto;
import com.worknest.features.attendance.dto.ClockAttendanceRequest;
import com.worknest.features.attendance.dto.ClockAttendanceResponse;
import com.worknest.features.attendance.dto.MonthlyAttendanceDayDto;
import com.worknest.features.attendance.dto.MonthlyAttendanceResponse;
import com.worknest.features.attendance.dto.TodayAttendanceResponse;
import com.worknest.features.attendance.repository.AttendanceDayRecordRepository;
import com.worknest.features.attendance.repository.AttendanceEventRepository;
import com.worknest.features.companySite.repository.SiteTrustedNetworkRepository;
import com.worknest.features.employee.repository.EmployeeRepository;
import com.worknest.security.AuthSessionPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MobileAttendanceService {

    private final EmployeeRepository employeeRepository;
    private final AttendancePolicyResolver attendancePolicyResolver;
    private final AttendanceQrService attendanceQrService;
    private final AttendanceEventRepository attendanceEventRepository;
    private final AttendanceDayRecordRepository attendanceDayRecordRepository;
    private final SiteTrustedNetworkRepository siteTrustedNetworkRepository;
    private final CidrMatcher cidrMatcher;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public TodayAttendanceResponse getToday() {
        Employee employee = resolveCurrentEmployee();
        Company company = employee.getCompany();
        CompanySite site = resolveEmployeeSite(employee);
        ResolvedAttendancePolicy policy = attendancePolicyResolver.resolveForSite(company, site);

        ZoneId zoneId = ZoneId.of(site.getTimezone());
        LocalDate workDate = ZonedDateTime.now(zoneId).toLocalDate();
        AttendanceDayRecord dayRecord = attendanceDayRecordRepository
                .findByCompanyIdAndEmployeeIdAndWorkDate(company.getId(), employee.getId(), workDate)
                .orElse(null);

        AttendanceState state = resolveState(dayRecord);
        NextAttendanceAction nextAction = resolveNextAction(state);

        return new TodayAttendanceResponse(
                state,
                nextAction,
                false,
                null,
                null,
                site.getId(),
                site.getName(),
                policy.dto().requireQr(),
                policy.dto().requireLocation(),
                Instant.now(),
                site.getTimezone(),
                workDate,
                mapDayRecord(dayRecord),
                List.of()
        );
    }

    public ClockAttendanceResponse clock(ClockAttendanceRequest request, HttpServletRequest httpRequest) {
        Employee employee = resolveCurrentEmployee();
        Company company = employee.getCompany();
        User user = employee.getUser();
        CompanySite site = resolveEmployeeSite(employee);
        ResolvedAttendancePolicy resolvedPolicy = attendancePolicyResolver.resolveForSite(company, site);
        AttendancePolicy policy = resolvedPolicy.entity();

        if (site.getStatus() != SiteStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.CONFLICT, "SITE_DISABLED", "Attendance site is not active.");
        }

        ZoneId zoneId = ZoneId.of(site.getTimezone());
        ZonedDateTime nowAtSite = ZonedDateTime.now(zoneId);
        LocalDate workDate = nowAtSite.toLocalDate();
        Instant serverNow = nowAtSite.toInstant();

        AttendanceEvent priorResult = attendanceEventRepository.findByCompanyIdAndClientRequestId(company.getId(), request.clientRequestId()).orElse(null);
        if (priorResult != null) {
            AttendanceDayRecord existingDay = attendanceDayRecordRepository
                    .findByCompanyIdAndEmployeeIdAndWorkDate(company.getId(), employee.getId(), workDate)
                    .orElse(null);
            return toClockResponse(priorResult, existingDay, site.getTimezone(), "Duplicate request replayed safely.");
        }

        AttendanceDayRecord dayRecord = attendanceDayRecordRepository
                .findWithLockByCompanyIdAndEmployeeIdAndWorkDate(company.getId(), employee.getId(), workDate)
                .orElse(null);

        AttendanceState currentState = resolveState(dayRecord);
        AttendanceEventType nextType = resolveNextEventType(currentState);
        validatePolicyAction(nextType, resolvedPolicy.dto());

        GeofenceResult geofenceResult = validateGeofence(policy, site, request, nextType);
        AttendanceQrChallenge qrChallenge = null;
        if (resolvedPolicy.dto().requireQr()) {
            qrChallenge = attendanceQrService.validateAndConsumeToken(request.qrToken(), company.getId(), site.getId(), user);
        }

        NetworkResult networkResult = evaluateNetwork(site, httpRequest);
        List<AttendanceWarningCode> warningCodes = new ArrayList<>();
        warningCodes.addAll(geofenceResult.warningCodes());
        warningCodes.addAll(networkResult.warningCodes());

        AttendanceDecision decision = warningCodes.isEmpty() ? AttendanceDecision.ACCEPTED : AttendanceDecision.ACCEPTED_WITH_WARNINGS;
        AttendanceEventStatus status = warningCodes.isEmpty() ? AttendanceEventStatus.ACCEPTED : AttendanceEventStatus.ACCEPTED_WITH_WARNINGS;

        AttendanceEvent event = new AttendanceEvent();
        event.setCompany(company);
        event.setEmployee(employee);
        event.setUser(user);
        event.setSite(site);
        event.setEventType(nextType);
        event.setEventStatus(status);
        event.setCaptureMethod(AttendanceCaptureMethod.QR_GEOFENCE);
        event.setAttendanceDecision(decision);
        event.setServerRecordedAt(serverNow);
        event.setClientCapturedAt(request.clientCapturedAt());
        event.setWorkDate(workDate);
        event.setTimezone(site.getTimezone());
        event.setClientRequestId(request.clientRequestId());
        event.setDevicePublicId(request.devicePublicId());
        event.setPlatform(request.platform());
        event.setAppVersion(request.appVersion());
        event.setQrChallenge(qrChallenge);
        event.setQrTerminal(qrChallenge != null ? qrChallenge.getQrTerminal() : null);
        event.setQrValidationStatus(qrChallenge != null
                ? com.worknest.domain.enums.AttendanceQrValidationStatus.VALID
                : com.worknest.domain.enums.AttendanceQrValidationStatus.NOT_REQUIRED);
        event.setLatitude(request.latitude());
        event.setLongitude(request.longitude());
        event.setAccuracyMeters(request.accuracyMeters());
        event.setDistanceFromSiteMeters(geofenceResult.distanceMeters());
        event.setInsideGeofence(geofenceResult.insideGeofence());
        event.setGeofenceDecision(geofenceResult.decision());
        event.setRequestIpAddress(networkResult.clientIp());
        event.setForwardedForChain(networkResult.forwardedForChain());
        event.setNetworkMatched(networkResult.networkMatched());
        event.setMatchedNetwork(networkResult.matchedNetwork());
        event.setNetworkDecision(networkResult.decision());
        event.setRiskScore(warningCodes.size() * 10);
        event.setWarningFlagsJson(writeWarningsJson(warningCodes));
        event.setEmployeeNote(request.employeeNote());
        event.setReviewStatus(AttendanceReviewStatus.NONE);

        AttendanceEvent savedEvent = attendanceEventRepository.save(event);
        AttendanceDayRecord updatedDayRecord = upsertDayRecord(dayRecord, savedEvent, employee, site, workDate, site.getTimezone(), warningCodes);
        return toClockResponse(savedEvent, updatedDayRecord, site.getTimezone(), "Attendance recorded successfully.");
    }

    @Transactional(readOnly = true)
    public MonthlyAttendanceResponse month(int year, int month) {
        Employee employee = resolveCurrentEmployee();
        CompanySite site = resolveEmployeeSite(employee);
        ZoneId zoneId = ZoneId.of(site.getTimezone());

        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        List<MonthlyAttendanceDayDto> days = attendanceDayRecordRepository
                .findAllByCompanyIdAndEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
                        employee.getCompany().getId(),
                        employee.getId(),
                        from,
                        to
                ).stream()
                .map(r -> new MonthlyAttendanceDayDto(
                        r.getWorkDate(),
                        r.getDayStatus(),
                        r.getFirstCheckInAt(),
                        r.getLastCheckOutAt(),
                        r.getWorkedMinutes() != null ? r.getWorkedMinutes() : 0,
                        Boolean.TRUE.equals(r.getHasWarnings()),
                        r.getReviewStatus()
                ))
                .toList();

        return new MonthlyAttendanceResponse(year, month, zoneId.getId(), days);
    }

    private AttendanceDayRecord upsertDayRecord(
            AttendanceDayRecord dayRecord,
            AttendanceEvent event,
            Employee employee,
            CompanySite site,
            LocalDate workDate,
            String timezone,
            List<AttendanceWarningCode> warningCodes
    ) {
        AttendanceDayRecord record = dayRecord;
        if (record == null) {
            record = new AttendanceDayRecord();
            record.setCompany(employee.getCompany());
            record.setEmployee(employee);
            record.setUser(employee.getUser());
            record.setSite(site);
            record.setWorkDate(workDate);
            record.setTimezone(timezone);
        }

        if (event.getEventType() == AttendanceEventType.CHECK_IN) {
            record.setFirstCheckInAt(event.getServerRecordedAt());
        } else if (event.getEventType() == AttendanceEventType.CHECK_OUT) {
            record.setLastCheckOutAt(event.getServerRecordedAt());
        }

        if (record.getFirstCheckInAt() != null && record.getLastCheckOutAt() != null) {
            long worked = Math.max(0L, java.time.Duration.between(record.getFirstCheckInAt(), record.getLastCheckOutAt()).toMinutes());
            record.setWorkedMinutes((int) worked);
            record.setDayStatus(AttendanceDayStatus.PRESENT);
        } else if (record.getFirstCheckInAt() != null) {
            record.setDayStatus(AttendanceDayStatus.PENDING_REVIEW);
        }

        record.setSourceEventCount((record.getSourceEventCount() != null ? record.getSourceEventCount() : 0) + 1);
        record.setHasWarnings(!warningCodes.isEmpty());
        record.setWarningFlagsJson(writeWarningsJson(warningCodes));
        return attendanceDayRecordRepository.save(record);
    }

    private void validatePolicyAction(AttendanceEventType nextType, com.worknest.features.attendance.dto.EffectiveAttendancePolicyDto policy) {
        if (nextType == AttendanceEventType.CHECK_IN && !policy.checkInEnabled()) {
            throw new BusinessException(HttpStatus.CONFLICT, "CHECKIN_DISABLED", "Check-in is disabled by policy.");
        }
        if (nextType == AttendanceEventType.CHECK_OUT && !policy.checkOutEnabled()) {
            throw new BusinessException(HttpStatus.CONFLICT, "CHECKOUT_DISABLED", "Check-out is disabled by policy.");
        }
    }

    private GeofenceResult validateGeofence(
            AttendancePolicy policy,
            CompanySite site,
            ClockAttendanceRequest request,
            AttendanceEventType nextType
    ) {
        List<AttendanceWarningCode> warnings = new ArrayList<>();

        if (!Boolean.TRUE.equals(policy.getRequireLocation())) {
            return new GeofenceResult(GeofenceDecision.NOT_REQUIRED, null, null, warnings);
        }

        if (request.latitude() == null || request.longitude() == null || request.accuracyMeters() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "LOCATION_REQUIRED", "Location coordinates and accuracy are required.");
        }

        double lat = request.latitude().doubleValue();
        double lon = request.longitude().doubleValue();
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_LOCATION", "Provided location is outside valid coordinate ranges.");
        }

        if (site.getGeofenceShapeType() == null) {
            return new GeofenceResult(GeofenceDecision.NOT_CONFIGURED, null, null, warnings);
        }

        if (site.getMaxLocationAccuracyMeters() != null
                && request.accuracyMeters().compareTo(BigDecimal.valueOf(site.getMaxLocationAccuracyMeters())) > 0) {
            if (Boolean.TRUE.equals(policy.getRejectPoorAccuracy())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "LOW_LOCATION_ACCURACY", "Location accuracy is too low.");
            }
            warnings.add(AttendanceWarningCode.LOW_LOCATION_ACCURACY);
        }

        if (site.getGeofenceShapeType() == com.worknest.domain.enums.GeofenceShapeType.CIRCLE) {
            if (site.getLatitude() == null || site.getLongitude() == null || site.getGeofenceRadiusMeters() == null) {
                throw new BusinessException(HttpStatus.CONFLICT, "GEOFENCE_NOT_CONFIGURED", "Site geofence is incomplete.");
            }
            double distance = calculateDistanceMeters(
                    site.getLatitude().doubleValue(),
                    site.getLongitude().doubleValue(),
                    lat,
                    lon
            );
            int radius = site.getGeofenceRadiusMeters();
            int buffer = nextType == AttendanceEventType.CHECK_IN
                    ? (site.getEntryBufferMeters() != null ? site.getEntryBufferMeters() : 0)
                    : (site.getExitBufferMeters() != null ? site.getExitBufferMeters() : 0);

            boolean inside = distance <= (radius + buffer);
            if (!inside && Boolean.TRUE.equals(policy.getRejectOutsideGeofence())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "OUTSIDE_GEOFENCE", "Clock action is outside site geofence.");
            }
            if (!inside) {
                warnings.add(AttendanceWarningCode.LOCATION_NEAR_BOUNDARY);
            }
            return new GeofenceResult(
                    inside ? GeofenceDecision.PASSED : GeofenceDecision.FAILED_OUTSIDE,
                    BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP),
                    inside,
                    warnings
            );
        }

        if (site.getGeofenceShapeType() == com.worknest.domain.enums.GeofenceShapeType.POLYGON) {
            if (site.getGeofencePolygonGeoJson() == null || site.getGeofencePolygonGeoJson().isBlank()) {
                throw new BusinessException(HttpStatus.CONFLICT, "GEOFENCE_NOT_CONFIGURED", "Site polygon geofence is missing.");
            }
            boolean inside = GeoJsonPolygonUtil.isPointInside(site.getGeofencePolygonGeoJson(), lat, lon);
            if (!inside && Boolean.TRUE.equals(policy.getRejectOutsideGeofence())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "OUTSIDE_GEOFENCE", "Clock action is outside site polygon.");
            }
            if (!inside) {
                warnings.add(AttendanceWarningCode.LOCATION_NEAR_BOUNDARY);
            }
            return new GeofenceResult(inside ? GeofenceDecision.PASSED : GeofenceDecision.FAILED_OUTSIDE, null, inside, warnings);
        }

        return new GeofenceResult(GeofenceDecision.NOT_CONFIGURED, null, null, warnings);
    }

    private NetworkResult evaluateNetwork(CompanySite site, HttpServletRequest request) {
        String clientIp = ClientIpResolver.resolve(request);
        String forwardedChain = request.getHeader("X-Forwarded-For");
        List<SiteTrustedNetwork> activeNetworks = siteTrustedNetworkRepository.findAllBySiteIdAndIsActiveTrueOrderByPriorityOrderAsc(site.getId());

        if (activeNetworks.isEmpty()) {
            return new NetworkResult(NetworkDecision.NOT_CONFIGURED, false, null, clientIp, forwardedChain, List.of(AttendanceWarningCode.NETWORK_NOT_CONFIGURED));
        }

        for (SiteTrustedNetwork network : activeNetworks) {
            if (cidrMatcher.matches(clientIp, network.getCidrBlock())) {
                return new NetworkResult(NetworkDecision.PASSED, true, network, clientIp, forwardedChain, List.of());
            }
        }
        return new NetworkResult(NetworkDecision.WARNING, false, null, clientIp, forwardedChain, List.of(AttendanceWarningCode.NETWORK_NOT_MATCHED));
    }

    private AttendanceRecordDto mapDayRecord(AttendanceDayRecord dayRecord) {
        if (dayRecord == null) {
            return null;
        }
        List<AttendanceWarningDto> warnings = extractWarnings(dayRecord.getWarningFlagsJson());
        return new AttendanceRecordDto(
                dayRecord.getId(),
                dayRecord.getFirstCheckInAt(),
                dayRecord.getLastCheckOutAt(),
                dayRecord.getWorkedMinutes() != null ? dayRecord.getWorkedMinutes() : 0,
                dayRecord.getDayStatus(),
                dayRecord.getReviewStatus(),
                Boolean.TRUE.equals(dayRecord.getHasWarnings()),
                warnings
        );
    }

    private ClockAttendanceResponse toClockResponse(AttendanceEvent event, AttendanceDayRecord dayRecord, String timezone, String message) {
        AttendanceState state = resolveState(dayRecord);
        NextAttendanceAction next = resolveNextAction(state);
        List<AttendanceWarningDto> warnings = extractWarnings(event.getWarningFlagsJson());
        return new ClockAttendanceResponse(
                state,
                next,
                event.getEventType(),
                event.getEventStatus(),
                event.getAttendanceDecision(),
                dayRecord != null ? dayRecord.getFirstCheckInAt() : null,
                dayRecord != null ? dayRecord.getLastCheckOutAt() : null,
                Instant.now(),
                event.getWorkDate(),
                timezone,
                warnings,
                message,
                mapDayRecord(dayRecord)
        );
    }

    private AttendanceState resolveState(AttendanceDayRecord dayRecord) {
        if (dayRecord == null || dayRecord.getFirstCheckInAt() == null) {
            return AttendanceState.NOT_CHECKED_IN;
        }
        if (dayRecord.getFirstCheckInAt() != null && dayRecord.getLastCheckOutAt() == null) {
            return AttendanceState.CHECKED_IN;
        }
        return AttendanceState.CHECKED_OUT;
    }

    private NextAttendanceAction resolveNextAction(AttendanceState state) {
        return switch (state) {
            case NOT_CHECKED_IN -> NextAttendanceAction.CHECK_IN;
            case CHECKED_IN -> NextAttendanceAction.CHECK_OUT;
            default -> NextAttendanceAction.NONE;
        };
    }

    private AttendanceEventType resolveNextEventType(AttendanceState state) {
        return switch (state) {
            case NOT_CHECKED_IN -> AttendanceEventType.CHECK_IN;
            case CHECKED_IN -> AttendanceEventType.CHECK_OUT;
            case CHECKED_OUT -> throw new BusinessException(HttpStatus.CONFLICT, "ALREADY_CHECKED_OUT", "Already checked out for today.");
            default -> throw new BusinessException(HttpStatus.CONFLICT, "ATTENDANCE_STATE_INVALID", "Attendance state requires manager review.");
        };
    }

    private Employee resolveCurrentEmployee() {
        AuthSessionPrincipal principal = resolvePrincipal();
        Employee employee = employeeRepository.findByUserIdAndCompanyId(principal.userId(), principal.companyId())
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "EMPLOYEE_PROFILE_NOT_FOUND", "Employee profile is not configured."));
        if (employee.getCompany().getStatus() != CompanyStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "COMPANY_INACTIVE", "Company is not active.");
        }
        if (employee.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "EMPLOYEE_INACTIVE", "Employee is not active.");
        }
        return employee;
    }

    private CompanySite resolveEmployeeSite(Employee employee) {
        CompanySite site = employee.getCompanySite();
        if (site == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "EMPLOYEE_SITE_NOT_ASSIGNED", "Employee is not assigned to a site.");
        }
        return site;
    }

    private AuthSessionPrincipal resolvePrincipal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthSessionPrincipal principal)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "No authentication session found.");
        }
        return principal;
    }

    private String writeWarningsJson(List<AttendanceWarningCode> warningCodes) {
        try {
            return objectMapper.writeValueAsString(warningCodes);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "WARNING_SERIALIZATION_FAILED", "Failed to serialize warning flags.");
        }
    }

    private List<AttendanceWarningDto> extractWarnings(String warningFlagsJson) {
        if (warningFlagsJson == null || warningFlagsJson.isBlank()) {
            return List.of();
        }
        try {
            List<String> codes = objectMapper.readValue(
                    warningFlagsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );
            List<AttendanceWarningDto> warnings = new ArrayList<>();
            for (String code : codes) {
                warnings.add(new AttendanceWarningDto(code, "LOW", code.replace('_', ' ').toLowerCase()));
            }
            return warnings;
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371000d;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private record GeofenceResult(
            GeofenceDecision decision,
            BigDecimal distanceMeters,
            Boolean insideGeofence,
            List<AttendanceWarningCode> warningCodes
    ) {
    }

    private record NetworkResult(
            NetworkDecision decision,
            boolean networkMatched,
            SiteTrustedNetwork matchedNetwork,
            String clientIp,
            String forwardedForChain,
            List<AttendanceWarningCode> warningCodes
    ) {
    }
}
