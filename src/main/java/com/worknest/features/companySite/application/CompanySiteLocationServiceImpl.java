package com.worknest.features.companySite.application;

import com.worknest.domain.entities.CompanySite;
import com.worknest.domain.enums.GeofenceShapeType;
import com.worknest.features.companySite.dto.DetectLocationRequest;
import com.worknest.features.companySite.dto.DetectLocationResponse;
import com.worknest.features.companySite.dto.LocationDetailsReadDto;
import com.worknest.features.companySite.dto.LocationDetailsUpdateRequest;
import com.worknest.features.companySite.exception.SiteNotFoundException;
import com.worknest.features.companySite.exception.StaleSiteDataException;
import com.worknest.features.companySite.repository.CompanySiteRepository;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

@Service
@Transactional
@RequiredArgsConstructor
public class CompanySiteLocationServiceImpl implements CompanySiteLocationService {

    private final CompanySiteRepository repository;

    @Override
    @Transactional(readOnly = true)
    public LocationDetailsReadDto getLocation(UUID companyId, UUID siteId) {
        CompanySite site = repository.findByIdAndCompanyId(siteId, companyId)
                .orElseThrow(SiteNotFoundException::new);
        return mapToDto(site);
    }

    @Override
    public LocationDetailsReadDto updateLocation(UUID companyId, UUID siteId, LocationDetailsUpdateRequest request) {
        CompanySite site = repository.findByIdAndCompanyId(siteId, companyId)
                .orElseThrow(SiteNotFoundException::new);

        if (!site.getVersion().equals(request.version())) {
            throw new StaleSiteDataException();
        }

        try {
            ZoneId.of(request.timezone());
        } catch (DateTimeException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_TIMEZONE", "Invalid timezone identifier.");
        }

        // Apply fields unconditionally
        site.setAddressLine1(request.addressLine1());
        site.setAddressLine2(request.addressLine2());
        site.setCity(request.city());
        site.setStateRegion(request.stateRegion());
        site.setPostalCode(request.postalCode());
        site.setCountryCode(request.countryCode());
        site.setTimezone(request.timezone());
        site.setLocationRequired(request.locationRequired());
        site.setMaxLocationAccuracyMeters(request.maxLocationAccuracyMeters());
        site.setEntryBufferMeters(request.entryBufferMeters());
        site.setExitBufferMeters(request.exitBufferMeters());

        // Strict normalization
        if (Boolean.FALSE.equals(request.locationRequired())) {
            site.setGeofenceShapeType(null);
            site.setLatitude(null);
            site.setLongitude(null);
            site.setGeofenceRadiusMeters(null);
            site.setGeofencePolygonGeoJson(null);
            site.setEntryBufferMeters(null);
            site.setExitBufferMeters(null);
        } else {
            if (request.geofenceShapeType() == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "MISSING_SHAPE", "Geofence shape type is required.");
            }
            site.setGeofenceShapeType(request.geofenceShapeType());

            if (request.geofenceShapeType() == GeofenceShapeType.CIRCLE) {
                if (request.latitude() == null || request.longitude() == null || request.geofenceRadiusMeters() == null) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_CIRCLE", "Circular geofence requires latitude, longitude, and radius.");
                }
                site.setLatitude(request.latitude());
                site.setLongitude(request.longitude());
                site.setGeofenceRadiusMeters(request.geofenceRadiusMeters());
                site.setGeofencePolygonGeoJson(null);
            } else if (request.geofenceShapeType() == GeofenceShapeType.POLYGON) {
                if (request.geofencePolygonGeoJson() == null || request.geofencePolygonGeoJson().isBlank()) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_POLYGON", "Polygon geofence requires GeoJSON.");
                }
                site.setGeofencePolygonGeoJson(request.geofencePolygonGeoJson());
                site.setLatitude(null);
                site.setLongitude(null);
                site.setGeofenceRadiusMeters(null);
            }
        }

        return mapToDto(repository.save(site));
    }

    @Override
    @Transactional(readOnly = true)
    public DetectLocationResponse assessLocation(UUID companyId, UUID siteId, DetectLocationRequest request) {
        CompanySite site = repository.findByIdAndCompanyId(siteId, companyId)
                .orElseThrow(SiteNotFoundException::new);
                
        List<String> warnings = new ArrayList<>();
        boolean isWithin = false;
        Double distance = null;

        if (Boolean.TRUE.equals(site.getLocationRequired()) && site.getGeofenceShapeType() == GeofenceShapeType.CIRCLE) {
            if (site.getLatitude() != null && site.getLongitude() != null) {
                distance = calculateDistanceMeters(
                    site.getLatitude().doubleValue(), site.getLongitude().doubleValue(),
                    request.latitude().doubleValue(), request.longitude().doubleValue()
                );
                isWithin = distance <= (site.getGeofenceRadiusMeters() != null ? site.getGeofenceRadiusMeters() : 100);
                if (!isWithin) {
                    warnings.add("The detected location is outside the established geofence radius.");
                }
            }
        }
        
        if (request.accuracyMeters() != null && site.getMaxLocationAccuracyMeters() != null) {
            if (request.accuracyMeters() > site.getMaxLocationAccuracyMeters()) {
                warnings.add("Detected accuracy (" + request.accuracyMeters() + "m) is worse than the maximum allowed (" + site.getMaxLocationAccuracyMeters() + "m).");
            }
        }
        
        String recommendedCountryCode = request.detectedCountryCode();
        if (recommendedCountryCode != null && site.getCountryCode() != null && !recommendedCountryCode.equalsIgnoreCase(site.getCountryCode())) {
            warnings.add("Detected country (" + recommendedCountryCode + ") does not match site configuration (" + site.getCountryCode() + ").");
        }

        return DetectLocationResponse.builder()
                .isWithinGeofence(isWithin)
                .distanceMeters(distance)
                .warnings(warnings)
                .recommendedCountryCode(recommendedCountryCode)
                .build();
    }
    
    // Haversine formula
    private double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private LocationDetailsReadDto mapToDto(CompanySite site) {
        return LocationDetailsReadDto.builder()
                .id(site.getId())
                .addressLine1(site.getAddressLine1())
                .addressLine2(site.getAddressLine2())
                .city(site.getCity())
                .stateRegion(site.getStateRegion())
                .postalCode(site.getPostalCode())
                .countryCode(site.getCountryCode())
                .timezone(site.getTimezone())
                .latitude(site.getLatitude())
                .longitude(site.getLongitude())
                .geofenceShapeType(site.getGeofenceShapeType())
                .geofenceRadiusMeters(site.getGeofenceRadiusMeters())
                .geofencePolygonGeoJson(site.getGeofencePolygonGeoJson())
                .entryBufferMeters(site.getEntryBufferMeters())
                .exitBufferMeters(site.getExitBufferMeters())
                .maxLocationAccuracyMeters(site.getMaxLocationAccuracyMeters())
                .locationRequired(site.getLocationRequired())
                .version(site.getVersion())
                .build();
    }
}
