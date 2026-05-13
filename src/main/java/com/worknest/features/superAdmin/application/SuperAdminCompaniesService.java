package com.worknest.features.superAdmin.application;

import com.worknest.common.api.PaginatedResponse;
import com.worknest.features.superAdmin.dto.CompanyRowDto;
import com.worknest.features.superAdmin.dto.SuspendCompanyRequest;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface SuperAdminCompaniesService {

    PaginatedResponse<CompanyRowDto> listCompanies(String search, String status, String plan, Pageable pageable);

    CompanyRowDto toggleSuspend(UUID companyId, SuspendCompanyRequest request);
}