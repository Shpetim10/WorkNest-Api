package com.worknest.features.superAdmin.dto;

public record CompanyRowDto(
        String id,
        String companyName,
        String legalName,
        String countryCode,
        String nipt,
        String registrationNumber,
        String email,
        String plan,
        long employeeCount,
        String status,
        String createdAt,
        String subscriptionEndDate,
        String subscriptionStatus,
        String trialEndsAt,
        String deactivationRequestedAt,
        String deletionScheduledAt
) {}
