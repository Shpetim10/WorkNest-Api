package com.worknest.features.subscription.application;

import com.worknest.features.subscription.dto.BillingPortalResponse;
import com.worknest.features.subscription.dto.PaymentMethodResponse;
import com.worknest.features.subscription.dto.ChangePlanRequest;
import com.worknest.features.subscription.dto.DeactivationStatusResponse;
import com.worknest.features.subscription.dto.PlanDetailsResponse;
import com.worknest.features.subscription.dto.StartTrialRequest;
import com.worknest.features.subscription.dto.StartTrialResponse;
import com.worknest.features.subscription.dto.SubscriptionResponse;
import java.util.List;
import java.util.UUID;

public interface SubscriptionService {

    StartTrialResponse startTrial(UUID companyId, StartTrialRequest request);

    SubscriptionResponse getCurrentSubscription(UUID companyId);

    SubscriptionResponse changePlan(UUID companyId, ChangePlanRequest request);

    BillingPortalResponse createBillingPortalSession(UUID companyId, String returnUrl);

    List<PlanDetailsResponse> getAllPlans();

    void handleStripeEvent(String payload, String sigHeader);

    SubscriptionResponse cancelSubscription(UUID companyId);

    PaymentMethodResponse getPaymentMethod(UUID companyId);

    void updatePaymentMethod(UUID companyId, String paymentMethodId);

    DeactivationStatusResponse deactivateAccount(UUID companyId);

    DeactivationStatusResponse reactivateAccount(UUID companyId);
}
