package com.worknest.features.subscription.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.features.subscription.application.SubscriptionService;
import com.worknest.features.subscription.dto.BillingPortalResponse;
import com.worknest.features.subscription.dto.ChangePlanRequest;
import com.worknest.features.subscription.dto.DeactivationStatusResponse;
import com.worknest.features.subscription.dto.PaymentMethodResponse;
import com.worknest.features.subscription.dto.UpdatePaymentMethodRequest;
import com.worknest.features.subscription.dto.PlanDetailsResponse;
import com.worknest.features.subscription.dto.StartTrialRequest;
import com.worknest.features.subscription.dto.StartTrialResponse;
import com.worknest.features.subscription.dto.SubscriptionResponse;
import com.worknest.security.AuthSessionPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Stripe-backed subscription management")
public class SubscriptionController {

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    private final SubscriptionService subscriptionService;

    @PostMapping("/start-trial")
    @Operation(summary = "Start a 30-day free trial", description = "Creates a Stripe customer and subscription with a 30-day trial. Card is required upfront but not charged until day 30.")
    public ApiResponse<StartTrialResponse> startTrial(
            @AuthenticationPrincipal AuthSessionPrincipal principal,
            @Valid @RequestBody StartTrialRequest request
    ) {
        StartTrialResponse response = subscriptionService.startTrial(principal.companyId(), request);
        return ApiResponse.success("Trial started successfully. Card will be charged after the 30-day trial.", response);
    }

    @GetMapping("/current")
    @Operation(summary = "Get current subscription", description = "Returns the current subscription plan and status for the authenticated company.")
    public ApiResponse<SubscriptionResponse> getCurrentSubscription(
            @AuthenticationPrincipal AuthSessionPrincipal principal
    ) {
        SubscriptionResponse response = subscriptionService.getCurrentSubscription(principal.companyId());
        return ApiResponse.success("Subscription retrieved successfully.", response);
    }

    @PutMapping("/plan")
    @Operation(summary = "Change subscription plan", description = "Upgrades or downgrades the current subscription plan via Stripe with proration.")
    public ApiResponse<SubscriptionResponse> changePlan(
            @AuthenticationPrincipal AuthSessionPrincipal principal,
            @Valid @RequestBody ChangePlanRequest request
    ) {
        SubscriptionResponse response = subscriptionService.changePlan(principal.companyId(), request);
        return ApiResponse.success("Plan changed successfully.", response);
    }

    @PostMapping("/billing-portal")
    @Operation(summary = "Open Stripe billing portal", description = "Returns a Stripe Billing Portal URL for the company to manage payment methods, invoices, and plan changes.")
    public ApiResponse<BillingPortalResponse> createBillingPortalSession(
            @AuthenticationPrincipal AuthSessionPrincipal principal,
            @RequestParam(required = false) String returnUrl
    ) {
        if (returnUrl == null || returnUrl.isBlank()) {
            returnUrl = frontendUrl + "/settings/billing";
        }
        BillingPortalResponse response = subscriptionService.createBillingPortalSession(principal.companyId(), returnUrl);
        return ApiResponse.success("Billing portal session created.", response);
    }

    @GetMapping("/plans")
    @Operation(summary = "List all plans", description = "Public endpoint — returns all available plan definitions and limits.")
    public ApiResponse<List<PlanDetailsResponse>> getAllPlans() {
        List<PlanDetailsResponse> plans = subscriptionService.getAllPlans();
        return ApiResponse.success("Plans retrieved successfully.", plans);
    }

    @PostMapping("/cancel")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('ADMIN')")
    @Operation(summary = "Cancel subscription", description = "Cancels the active Stripe subscription immediately. The account remains accessible but billing stops.")
    public ApiResponse<SubscriptionResponse> cancelSubscription(
            @AuthenticationPrincipal AuthSessionPrincipal principal
    ) {
        SubscriptionResponse response = subscriptionService.cancelSubscription(principal.companyId());
        return ApiResponse.success("Subscription canceled.", response);
    }

    @GetMapping("/payment-method")
    @Operation(summary = "Get saved payment method", description = "Returns the card brand, last 4 digits, and expiry for the company's default Stripe payment method.")
    public ApiResponse<PaymentMethodResponse> getPaymentMethod(
            @AuthenticationPrincipal AuthSessionPrincipal principal
    ) {
        PaymentMethodResponse response = subscriptionService.getPaymentMethod(principal.companyId());
        return ApiResponse.success("Payment method retrieved.", response);
    }

    @PostMapping("/payment-method")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('ADMIN')")
    @Operation(summary = "Update payment method", description = "Attaches a new card as the default payment method for the company's Stripe customer.")
    public ApiResponse<Void> updatePaymentMethod(
            @AuthenticationPrincipal AuthSessionPrincipal principal,
            @Valid @RequestBody UpdatePaymentMethodRequest request
    ) {
        subscriptionService.updatePaymentMethod(principal.companyId(), request.paymentMethodId());
        return ApiResponse.success("Payment method updated.", null);
    }

    @PostMapping("/deactivate")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('ADMIN')")
    @Operation(summary = "Request account deactivation", description = "Schedules the company for deletion in 30 days. The company remains accessible during this period.")
    public ApiResponse<DeactivationStatusResponse> deactivateAccount(
            @AuthenticationPrincipal AuthSessionPrincipal principal
    ) {
        DeactivationStatusResponse response = subscriptionService.deactivateAccount(principal.companyId());
        return ApiResponse.success("Deactivation requested. Your account will be deleted in 30 days.", response);
    }

    @PostMapping("/reactivate")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('ADMIN')")
    @Operation(summary = "Cancel account deactivation", description = "Cancels a pending deactivation request, keeping the company active.")
    public ApiResponse<DeactivationStatusResponse> reactivateAccount(
            @AuthenticationPrincipal AuthSessionPrincipal principal
    ) {
        DeactivationStatusResponse response = subscriptionService.reactivateAccount(principal.companyId());
        return ApiResponse.success("Deactivation cancelled. Your account is now active.", response);
    }
}
