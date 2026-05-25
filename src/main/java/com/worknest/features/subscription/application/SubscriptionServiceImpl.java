package com.worknest.features.subscription.application;

import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.PlanLimit;
import com.worknest.domain.entities.Subscription;
import com.worknest.domain.enums.SubscriptionPlan;
import com.worknest.domain.enums.SubscriptionStatus;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.subscription.dto.BillingPortalResponse;
import com.worknest.features.subscription.dto.PlanDetailsResponse;
import com.worknest.features.subscription.dto.StartTrialRequest;
import com.worknest.features.subscription.dto.StartTrialResponse;
import com.worknest.features.subscription.dto.SubscriptionResponse;
import com.worknest.features.subscription.exception.SubscriptionNotFoundException;
import com.worknest.features.subscription.repository.PlanLimitRepository;
import com.worknest.features.subscription.repository.SubscriptionRepository;
import java.time.Instant;
import java.util.List;
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
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanLimitRepository planLimitRepository;
    private final CompanyRepository companyRepository;
    private final StripeService stripeService;

    @Value("${stripe.prices.foundation}")
    private String priceFoundation;

    @Value("${stripe.prices.growth}")
    private String priceGrowth;

    @Value("${stripe.prices.professional}")
    private String priceProfessional;

    @Override
    @Transactional
    public StartTrialResponse startTrial(UUID companyId, StartTrialRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        String priceId = resolvePriceId(request.plan());

        Customer stripeCustomer = stripeService.createCustomer(
                company.getEmail(), company.getName(), companyId.toString());

        com.stripe.model.Subscription stripeSub = stripeService.createSubscriptionWithTrial(
                stripeCustomer.getId(), priceId, request.paymentMethodId());

        Instant trialEndsAt = Instant.now().plusSeconds(30L * 24 * 60 * 60);
        Instant currentPeriodEnd = stripeSub.getCurrentPeriodEnd() != null
                ? Instant.ofEpochSecond(stripeSub.getCurrentPeriodEnd())
                : trialEndsAt;

        Subscription subscription = new Subscription();
        subscription.setCompanyId(companyId);
        subscription.setStripeCustomerId(stripeCustomer.getId());
        subscription.setStripeSubscriptionId(stripeSub.getId());
        subscription.setPlan(request.plan());
        subscription.setStatus(SubscriptionStatus.TRIALING);
        subscription.setTrialEndsAt(trialEndsAt);
        subscription.setCurrentPeriodEnd(currentPeriodEnd);

        Subscription saved = subscriptionRepository.save(subscription);

        String clientSecret = extractClientSecret(stripeSub);

        return new StartTrialResponse(
                saved.getId(),
                saved.getPlan(),
                saved.getStatus(),
                saved.getTrialEndsAt(),
                clientSecret
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse getCurrentSubscription(UUID companyId) {
        Subscription sub = subscriptionRepository
                .findTopByCompanyIdAndStatusInOrderByCreatedAtDesc(
                        companyId,
                        List.of(SubscriptionStatus.TRIALING, SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE))
                .or(() -> subscriptionRepository.findTopByCompanyIdOrderByCreatedAtDesc(companyId))
                .orElseThrow(SubscriptionNotFoundException::new);

        return new SubscriptionResponse(
                sub.getId(),
                sub.getPlan(),
                sub.getStatus(),
                sub.getTrialEndsAt(),
                sub.getCurrentPeriodEnd()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public BillingPortalResponse createBillingPortalSession(UUID companyId, String returnUrl) {
        Subscription sub = subscriptionRepository
                .findTopByCompanyIdOrderByCreatedAtDesc(companyId)
                .orElseThrow(SubscriptionNotFoundException::new);

        com.stripe.model.billingportal.Session session =
                stripeService.createBillingPortalSession(sub.getStripeCustomerId(), returnUrl);

        return new BillingPortalResponse(session.getUrl());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanDetailsResponse> getAllPlans() {
        return planLimitRepository.findAll().stream()
                .map(this::toPlanDetailsResponse)
                .toList();
    }

    @Override
    @Transactional
    public void handleStripeEvent(String payload, String sigHeader) {
        Event event = stripeService.constructWebhookEvent(payload, sigHeader);

        switch (event.getType()) {
            case "customer.subscription.updated" -> handleSubscriptionUpdated(event);
            case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);
            case "invoice.payment_failed" -> handlePaymentFailed(event);
            default -> log.debug("Unhandled Stripe event type: {}", event.getType());
        }
    }

    private void handleSubscriptionUpdated(Event event) {
        deserializeSubscription(event).ifPresent(stripeSub -> {
            subscriptionRepository.findByStripeSubscriptionId(stripeSub.getId()).ifPresent(sub -> {
                sub.setStatus(mapStripeStatus(stripeSub.getStatus()));
                if (stripeSub.getCurrentPeriodEnd() != null) {
                    sub.setCurrentPeriodEnd(Instant.ofEpochSecond(stripeSub.getCurrentPeriodEnd()));
                }
                subscriptionRepository.save(sub);
                log.info("Subscription {} updated to status {}", stripeSub.getId(), stripeSub.getStatus());
            });
        });
    }

    private void handleSubscriptionDeleted(Event event) {
        deserializeSubscription(event).ifPresent(stripeSub -> {
            subscriptionRepository.findByStripeSubscriptionId(stripeSub.getId()).ifPresent(sub -> {
                sub.setStatus(SubscriptionStatus.CANCELED);
                subscriptionRepository.save(sub);
                log.info("Subscription {} canceled", stripeSub.getId());
            });
        });
    }

    private void handlePaymentFailed(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        deserializer.getObject().ifPresent(obj -> {
            if (obj instanceof com.stripe.model.Invoice invoice) {
                String subId = invoice.getSubscription();
                if (subId != null) {
                    subscriptionRepository.findByStripeSubscriptionId(subId).ifPresent(sub -> {
                        sub.setStatus(SubscriptionStatus.PAST_DUE);
                        subscriptionRepository.save(sub);
                        log.warn("Payment failed for subscription {}", subId);
                    });
                }
            }
        });
    }

    private Optional<com.stripe.model.Subscription> deserializeSubscription(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Optional<StripeObject> obj = deserializer.getObject();
        if (obj.isPresent() && obj.get() instanceof com.stripe.model.Subscription s) {
            return Optional.of(s);
        }
        return Optional.empty();
    }

    private SubscriptionStatus mapStripeStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "trialing" -> SubscriptionStatus.TRIALING;
            case "active" -> SubscriptionStatus.ACTIVE;
            case "past_due" -> SubscriptionStatus.PAST_DUE;
            case "canceled", "cancelled" -> SubscriptionStatus.CANCELED;
            default -> SubscriptionStatus.PAST_DUE;
        };
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

    private PlanDetailsResponse toPlanDetailsResponse(PlanLimit limit) {
        return new PlanDetailsResponse(
                limit.getPlan(),
                limit.getPriceMonthlyInCents(),
                limit.getMaxEmployees(),
                limit.getMaxManagers(),
                limit.getMaxDepartments(),
                limit.getMaxLocations(),
                limit.getPayrollEnabled(),
                limit.getAuditLogsEnabled()
        );
    }
}
