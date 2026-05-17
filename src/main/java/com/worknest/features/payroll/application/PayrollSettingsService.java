package com.worknest.features.payroll.application;

import com.worknest.features.payroll.dto.PayrollDtos.PayrollSettingsResponse;
import com.worknest.features.payroll.dto.PayrollDtos.PublicHolidayRequest;
import com.worknest.features.payroll.dto.PayrollDtos.PublicHolidayResponse;
import com.worknest.features.payroll.dto.PayrollDtos.ReplaceTaxBracketsRequest;
import com.worknest.features.payroll.dto.PayrollDtos.TaxBracketResponse;
import com.worknest.features.payroll.dto.PayrollDtos.UpsertPayrollSettingsRequest;
import java.util.List;
import java.util.UUID;

public interface PayrollSettingsService {

    PayrollSettingsResponse getSettings();

    PayrollSettingsResponse upsertSettings(UpsertPayrollSettingsRequest request);

    List<TaxBracketResponse> getTaxBrackets();

    List<TaxBracketResponse> replaceTaxBrackets(ReplaceTaxBracketsRequest request);

    List<PublicHolidayResponse> getHolidays(int year);

    PublicHolidayResponse createHoliday(PublicHolidayRequest request);

    PublicHolidayResponse updateHoliday(UUID id, PublicHolidayRequest request);

    void deleteHoliday(UUID id);
}
