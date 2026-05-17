package com.worknest.features.payroll.application;

import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.PayrollResult;
import com.worknest.domain.enums.PayrollStatus;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.employee.repository.EmployeeRepository;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollCalculationResponse;
import com.worknest.features.payroll.repository.PayrollResultRepository;
import com.worknest.security.AuthSessionPrincipal;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Generates payslip documents from the payroll snapshot.
 *
 * Current implementation renders the Thymeleaf template and returns UTF-8 HTML bytes.
 * To produce real PDF output, add openhtmltopdf-core + openhtmltopdf-pdfbox 1.0.10 to pom.xml
 * and replace renderToBytes() with PdfRendererBuilder. See PAYROLL_REVIEW.md for the decision.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayslipPdfService {

    private static final Set<PayrollStatus> LOCKED_STATUSES = Set.of(
            PayrollStatus.APPROVED, PayrollStatus.FINALIZED, PayrollStatus.PAID);

    private final PayrollResultRepository resultRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final PayrollServiceImpl payrollService;
    private final TemplateEngine templateEngine;

    public byte[] generateForCurrentEmployee(int year, int month) {
        AuthSessionPrincipal principal = principal();
        Employee employee = employeeRepository.findByUserIdAndCompanyId(principal.userId(), principal.companyId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "EMPLOYEE_PROFILE_NOT_FOUND",
                        "Employee profile not found."));
        return generate(principal.companyId(), employee, year, month);
    }

    public byte[] generateForEmployee(UUID employeeId, int year, int month) {
        AuthSessionPrincipal principal = principal();
        Employee employee = employeeRepository.findByIdAndCompanyId(employeeId, principal.companyId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND",
                        "Employee not found."));
        return generate(principal.companyId(), employee, year, month);
    }

    private byte[] generate(UUID companyId, Employee employee, int year, int month) {
        PayrollResult result = resultRepository.findByCompanyIdAndEmployeeIdAndYearAndMonth(
                        companyId, employee.getId(), year, month)
                .orElseThrow(() -> new BusinessException(HttpStatus.CONFLICT, "PAYROLL_NOT_CALCULATED",
                        "Payroll has not been calculated for this period."));

        if (result.getStatus() == PayrollStatus.DRAFT) {
            throw new BusinessException(HttpStatus.CONFLICT, "PAYROLL_NOT_CALCULATED",
                    "Payroll is in DRAFT status. Calculate payroll first.");
        }

        PayrollCalculationResponse payroll = LOCKED_STATUSES.contains(result.getStatus())
                ? payrollService.responseFromSnapshot(result, false)
                : payrollService.previewCurrentEmployeePayroll(year, month);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND",
                        "Company not found."));

        Context ctx = new Context();
        ctx.setVariable("company", company);
        ctx.setVariable("employee", employee);
        ctx.setVariable("payroll", payroll);
        ctx.setVariable("year", year);
        ctx.setVariable("month", month);
        ctx.setVariable("generatedAt", java.time.Instant.now());

        String html = templateEngine.process("payroll/payslip", ctx);
        return renderToBytes(html);
    }

    /**
     * Converts the rendered HTML to a byte array.
     * Replace this with openhtmltopdf PdfRendererBuilder once the library is added to pom.xml.
     */
    private byte[] renderToBytes(String html) {
        // Stub: returns HTML bytes. Replace with actual PDF rendering once openhtmltopdf is available.
        return html.getBytes(StandardCharsets.UTF_8);
    }

    private AuthSessionPrincipal principal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthSessionPrincipal p)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "No authentication session found.");
        }
        return p;
    }
}
