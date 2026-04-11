package com.worknest.features.companySite.application;

import com.worknest.domain.entities.CompanySite;
import com.worknest.domain.enums.SiteStatus;
import com.worknest.features.companySite.dto.MainDetailsReadDto;
import com.worknest.features.companySite.dto.MainDetailsUpdateRequest;
import com.worknest.features.companySite.exception.InvalidStatusTransitionException;
import com.worknest.features.companySite.exception.SiteCodeAlreadyExistsException;
import com.worknest.features.companySite.exception.SiteNotFoundException;
import com.worknest.features.companySite.exception.StaleSiteDataException;
import com.worknest.features.companySite.repository.CompanySiteRepository;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

@Service
@Transactional
@RequiredArgsConstructor
public class CompanySiteUpdateServiceImpl implements CompanySiteUpdateService {

    private final CompanySiteRepository repository;

    @Override
    @Transactional(readOnly = true)
    public MainDetailsReadDto getMainDetails(UUID companyId, UUID siteId) {
        CompanySite site = repository.findByIdAndCompanyId(siteId, companyId)
                .orElseThrow(SiteNotFoundException::new);
        return mapToDto(site);
    }

    @Override
    public MainDetailsReadDto updateMainDetails(UUID companyId, UUID siteId, MainDetailsUpdateRequest request) {
        CompanySite site = repository.findByIdAndCompanyId(siteId, companyId)
                .orElseThrow(SiteNotFoundException::new);

        // 1. Optimistic Locking Validate
        if (!site.getVersion().equals(request.version())) {
            throw new StaleSiteDataException();
        }

        // 2. Uniqueness Check
        if (!site.getCode().equalsIgnoreCase(request.code()) &&
            repository.existsByCompanyIdAndCodeIgnoreCaseAndIdNot(companyId, request.code(), siteId)) {
            throw new SiteCodeAlreadyExistsException(request.code());
        }

        // 3. Timezone Validation
        try {
            ZoneId.of(request.timezone());
        } catch (DateTimeException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_TIMEZONE", "Invalid timezone identifier.");
        }

        // 4. Status Transition Guard
        guardStatusTransition(site.getStatus(), request.status());

        // 5. Apply Updates
        site.setCode(request.code());
        site.setName(request.name());
        site.setType(request.type());
        site.setStatus(request.status());
        site.setCountryCode(request.countryCode());
        site.setTimezone(request.timezone());
        site.setNotes(request.notes());
        site.setQrEnabled(request.qrEnabled());
        site.setCheckInEnabled(request.checkInEnabled());
        site.setCheckOutEnabled(request.checkOutEnabled());

        CompanySite updatedSite = repository.save(site);
        return mapToDto(updatedSite);
    }

    private void guardStatusTransition(SiteStatus current, SiteStatus target) {
        if (current == target) return;

        if (target == SiteStatus.ACTIVE && current != SiteStatus.DISABLED) {
            throw new InvalidStatusTransitionException("Cannot directly activate site via updates. Use the explicit activation flow.");
        }
        
        // Let them disable it or keep it disabled
    }

    private MainDetailsReadDto mapToDto(CompanySite site) {
        return MainDetailsReadDto.builder()
                .id(site.getId())
                .code(site.getCode())
                .name(site.getName())
                .type(site.getType())
                .status(site.getStatus())
                .countryCode(site.getCountryCode())
                .timezone(site.getTimezone())
                .notes(site.getNotes())
                .qrEnabled(site.getQrEnabled())
                .checkInEnabled(site.getCheckInEnabled())
                .checkOutEnabled(site.getCheckOutEnabled())
                .version(site.getVersion())
                .build();
    }
}
