package com.worknest.features.companySite.application;

import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.CompanySite;
import com.worknest.domain.entities.SiteTrustedNetwork;
import com.worknest.features.companySite.dto.TrustedNetworkResponse;
import com.worknest.features.companySite.dto.UpdateNetworkRequest;
import com.worknest.features.companySite.exception.DuplicateTrustedNetworkException;
import com.worknest.features.companySite.exception.SiteNotFoundException;
import com.worknest.features.companySite.exception.StaleSiteDataException;
import com.worknest.features.companySite.repository.CompanySiteRepository;
import com.worknest.features.companySite.repository.SiteTrustedNetworkRepository;
import com.worknest.features.companySite.validation.CidrValidator;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class SiteTrustedNetworkServiceImpl implements SiteTrustedNetworkService {

    private final SiteTrustedNetworkRepository networkRepository;
    private final CompanySiteRepository siteRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<TrustedNetworkResponse> listNetworks(UUID companyId, UUID siteId, Pageable pageable) {
        verifySiteOwnership(companyId, siteId);
        return networkRepository.findAllBySiteIdOrderByPriorityOrderAsc(siteId, pageable)
                .map(TrustedNetworkResponse::fromEntity);
    }

    @Override
    public TrustedNetworkResponse updateNetwork(UUID companyId, UUID siteId, UUID networkId, UpdateNetworkRequest request) {
        verifySiteOwnership(companyId, siteId);

        
        SiteTrustedNetwork network = networkRepository.findByIdAndSiteId(networkId, siteId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NETWORK_NOT_FOUND", "Network not found."));

        if (!network.getVersion().equals(request.version())) {
            throw new StaleSiteDataException();
        }

        if (request.expiresAt() != null && request.expiresAt().isBefore(Instant.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_EXPIRATION", "Network expiration time must be in the future.");
        }

        String normalizedCidr = CidrValidator.normalize(request.cidrBlock());
        CidrValidator.validate(normalizedCidr, CidrValidator.resolveIpVersion(normalizedCidr));

        if (networkRepository.existsBySiteIdAndCidrBlockAndNetworkTypeAndIdNot(siteId, normalizedCidr, request.networkType(), networkId)) {
            throw new DuplicateTrustedNetworkException(normalizedCidr);
        }

        network.setName(request.name().trim());
        network.setNetworkType(request.networkType());
        network.setCidrBlock(normalizedCidr);
        network.setIpVersion(CidrValidator.resolveIpVersion(normalizedCidr));
        network.setNotes(request.notes());
        network.setExpiresAt(request.expiresAt());
        network.setIsActive(request.isActive());

        return TrustedNetworkResponse.fromEntity(networkRepository.save(network));
    }



    private CompanySite verifySiteOwnership(UUID companyId, UUID siteId) {
        return siteRepository.findByIdAndCompanyId(siteId, companyId)
                .orElseThrow(SiteNotFoundException::new);
    }
}
