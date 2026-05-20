package com.worknest.features.subscription.application;

import com.stripe.model.Customer;
import com.worknest.common.plan.TrialInitializationService;
import com.worknest.domain.entities.Subscription;
import com.worknest.domain.enums.SubscriptionPlan;
import com.worknest.domain.enums.SubscriptionStatus;
import com.worknest.features.subscription.repository.SubscriptionRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrialInitializationServiceImpl implements TrialInitializationService {

    private final StripeService stripeService;
    private final SubscriptionRepository subscriptionRepository;

    @Value("${stripe.prices.foundation}")
    private String priceFoundation;

    @Value("${stripe.prices.growth}")
    private String priceGrowth;

    @Value("${stripe.prices.professional}")
    private String priceProfessional;

    @Override
    @Transactional
    public Optional<String> initializeTrial(
            UUID companyId,
            String companyName,
            String companyEmail,
            SubscriptionPlan plan,
            String paymentMethodId
    ) {
        String priceId = resolvePriceId(plan);

        Customer stripeCustomer = stripeService.createCustomer(companyEmail, companyName, companyId.toString());

        com.stripe.model.Subscription stripeSub = stripeService.createSubscriptionWithTrial(
                stripeCustomer.getId(), priceId, paymentMethodId);

        Instant trialEndsAt = Instant.now().plusSeconds(30L * 24 * 60 * 60);
        Instant currentPeriodEnd = stripeSub.getCurrentPeriodEnd() != null
                ? Instant.ofEpochSecond(stripeSub.getCurrentPeriodEnd())
                : trialEndsAt;

        Subscription subscription = new Subscription();
        subscription.setCompanyId(companyId);
        subscription.setStripeCustomerId(stripeCustomer.getId());
        subscription.setStripeSubscriptionId(stripeSub.getId());
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.TRIALING);
        subscription.setTrialEndsAt(trialEndsAt);
        subscription.setCurrentPeriodEnd(currentPeriodEnd);

        subscriptionRepository.save(subscription);
        log.info("Trial subscription created for company {} on plan {}", companyId, plan);

        return Optional.ofNullable(extractClientSecret(stripeSub));
    }

    private String resolvePriceId(SubscriptionPlan plan) {
        return switch (plan) {
            case FOUNDATION -> priceFoundation;
            case GROWTH -> priceGrowth;
            case PROFESSIONAL -> priceProfessional;
            default -> throw new IllegalArgumentException("Unsupported plan for trial: " + plan);
        };
    }

    private String extractClientSecret(com.stripe.model.Subscription stripeSub) {
        try {
            if (stripeSub.getLatestInvoiceObject() != null
                    && stripeSub.getLatestInvoiceObject().getPaymentIntentObject() != null) {
                return stripeSub.getLatestInvoiceObject().getPaymentIntentObject().getClientSecret();
            }
        } catch (Exception e) {
            log.warn("Could not extract client secret from Stripe subscription: {}", e.getMessage());
        }
        return null;
    }
}
