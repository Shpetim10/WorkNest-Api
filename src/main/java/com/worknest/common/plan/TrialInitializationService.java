package com.worknest.common.plan;

import com.worknest.domain.enums.SubscriptionPlan;
import java.util.Optional;
import java.util.UUID;

public interface TrialInitializationService {

    /**
     * Creates a Stripe customer + trial subscription for a newly-registered company.
     * Called during company registration when the user provides a plan and payment method.
     *
     * @return the Stripe client secret required for frontend card-setup confirmation,
     *         or empty if the secret could not be extracted.
     */
    Optional<String> initializeTrial(
            UUID companyId,
            String companyName,
            String companyEmail,
            SubscriptionPlan plan,
            String paymentMethodId
    );
}
