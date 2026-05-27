package com.worknest.features.subscription.application;

import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.PlanLimit;
import com.worknest.domain.entities.Subscription;
import com.worknest.domain.enums.CompanyStatus;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.domain.enums.SubscriptionPlan;
import com.worknest.domain.enums.SubscriptionStatus;
import com.worknest.audit.domain.AuditLog;
import com.worknest.audit.service.AuditLogService;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.common.exception.BusinessException;
import com.worknest.features.subscription.dto.BillingPortalResponse;
import com.worknest.features.subscription.dto.PaymentMethodResponse;
import com.worknest.features.subscription.dto.ChangePlanRequest;
import com.worknest.features.subscription.dto.DeactivationStatusResponse;
import com.worknest.features.subscription.dto.PlanDetailsResponse;
import com.worknest.features.subscription.dto.StartTrialRequest;
import com.worknest.features.subscription.dto.StartTrialResponse;
import com.worknest.features.subscription.dto.SubscriptionResponse;
import com.worknest.features.subscription.exception.SubscriptionNotFoundException;
import com.worknest.features.subscription.repository.PlanLimitRepository;
import com.worknest.features.subscription.repository.SubscriptionRepository;
import com.worknest.security.AuthSessionPrincipal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final AuditLogService auditLogService;

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
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND", "Company not found"));

        Optional<Subscription> subOpt = subscriptionRepository
                .findTopByCompanyIdAndStatusInOrderByCreatedAtDesc(
                        companyId,
                        List.of(SubscriptionStatus.TRIALING, SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE))
                .or(() -> subscriptionRepository.findTopByCompanyIdOrderByCreatedAtDesc(companyId));

        if (subOpt.isPresent()) {
            Subscription sub = subOpt.get();
            return new SubscriptionResponse(
                    sub.getId(),
                    sub.getPlan(),
                    sub.getStatus(),
                    sub.getTrialEndsAt(),
                    sub.getCurrentPeriodEnd(),
                    company.getDeactivationRequestedAt(),
                    company.getDeletionScheduledAt()
            );
        }

        // Fall back to Company entity for companies created before Stripe integration
        SubscriptionPlan plan = company.getSubscriptionPlan() != null
                ? company.getSubscriptionPlan()
                : SubscriptionPlan.FOUNDATION;

        return new SubscriptionResponse(
                null,
                plan,
                company.getSubscriptionStatus() != null ? company.getSubscriptionStatus() : SubscriptionStatus.TRIALING,
                company.getTrialEndsAt(),
                company.getSubscriptionRenewalAt(),
                company.getDeactivationRequestedAt(),
                company.getDeletionScheduledAt()
        );
    }

    @Override
    @Transactional
    public SubscriptionResponse changePlan(UUID companyId, ChangePlanRequest request) {
        Optional<Subscription> activeSubOpt = subscriptionRepository
                .findTopByCompanyIdAndStatusInOrderByCreatedAtDesc(
                        companyId,
                        List.of(SubscriptionStatus.TRIALING, SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE));

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        if (activeSubOpt.isPresent()) {
            Subscription sub = activeSubOpt.get();
            if (sub.getPlan() == request.newPlan()) {
                throw new IllegalArgumentException("Already on plan: " + request.newPlan());
            }
            String newPriceId = resolvePriceId(request.newPlan());
            stripeService.updateSubscriptionPlan(sub.getStripeSubscriptionId(), newPriceId);
            sub.setPlan(request.newPlan());
            Subscription saved = subscriptionRepository.save(sub);
            company.setSubscriptionPlan(request.newPlan());
            companyRepository.save(company);
            log.info("Plan changed to {} for company {}", request.newPlan(), companyId);
            return new SubscriptionResponse(saved.getId(), saved.getPlan(), saved.getStatus(),
                    saved.getTrialEndsAt(), saved.getCurrentPeriodEnd(), null, null);
        }

        // No active subscription — resubscribe from a previously canceled subscription
        Subscription canceledSub = subscriptionRepository
                .findTopByCompanyIdOrderByCreatedAtDesc(companyId)
                .orElseThrow(SubscriptionNotFoundException::new);

        String priceId = resolvePriceId(request.newPlan());
        com.stripe.model.Subscription newStripeSub =
                stripeService.createSubscriptionImmediate(canceledSub.getStripeCustomerId(), priceId);

        Instant currentPeriodEnd = newStripeSub.getCurrentPeriodEnd() != null
                ? Instant.ofEpochSecond(newStripeSub.getCurrentPeriodEnd())
                : Instant.now().plus(30, ChronoUnit.DAYS);

        Subscription newSub = new Subscription();
        newSub.setCompanyId(companyId);
        newSub.setStripeCustomerId(canceledSub.getStripeCustomerId());
        newSub.setStripeSubscriptionId(newStripeSub.getId());
        newSub.setPlan(request.newPlan());
        newSub.setStatus(SubscriptionStatus.ACTIVE);
        newSub.setCurrentPeriodEnd(currentPeriodEnd);
        Subscription saved = subscriptionRepository.save(newSub);

        company.setSubscriptionPlan(request.newPlan());
        companyRepository.save(company);

        log.info("Resubscribed to {} for company {}", request.newPlan(), companyId);
        return new SubscriptionResponse(saved.getId(), saved.getPlan(), saved.getStatus(),
                null, saved.getCurrentPeriodEnd(), null, null);
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
    @Transactional(readOnly = true)
    public PaymentMethodResponse getPaymentMethod(UUID companyId) {
        Subscription sub = subscriptionRepository
                .findTopByCompanyIdAndStatusInOrderByCreatedAtDesc(
                        companyId,
                        List.of(SubscriptionStatus.TRIALING, SubscriptionStatus.ACTIVE,
                                SubscriptionStatus.PAST_DUE, SubscriptionStatus.CANCELED))
                .orElseThrow(SubscriptionNotFoundException::new);

        com.stripe.model.PaymentMethod pm = stripeService.getDefaultPaymentMethod(sub.getStripeCustomerId());
        if (pm == null || pm.getCard() == null) {
            return null;
        }

        return new PaymentMethodResponse(
                pm.getCard().getBrand(),
                pm.getCard().getLast4(),
                pm.getCard().getExpMonth().intValue(),
                pm.getCard().getExpYear().intValue()
        );
    }

    @Override
    @Transactional
    public SubscriptionResponse cancelSubscription(UUID companyId) {
        Subscription sub = subscriptionRepository
                .findTopByCompanyIdAndStatusInOrderByCreatedAtDesc(
                        companyId,
                        List.of(SubscriptionStatus.TRIALING, SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE))
                .orElseThrow(SubscriptionNotFoundException::new);

        stripeService.cancelSubscription(sub.getStripeSubscriptionId());
        sub.setStatus(SubscriptionStatus.CANCELED);
        Subscription saved = subscriptionRepository.save(sub);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND", "Company not found"));

        log.info("Subscription canceled for company {}", companyId);
        return new SubscriptionResponse(
                saved.getId(), saved.getPlan(), saved.getStatus(),
                saved.getTrialEndsAt(), saved.getCurrentPeriodEnd(),
                company.getDeactivationRequestedAt(), company.getDeletionScheduledAt()
        );
    }

    @Override
    @Transactional
    public void updatePaymentMethod(UUID companyId, String paymentMethodId) {
        Subscription sub = subscriptionRepository
                .findTopByCompanyIdAndStatusInOrderByCreatedAtDesc(
                        companyId,
                        List.of(SubscriptionStatus.TRIALING, SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE))
                .orElseThrow(SubscriptionNotFoundException::new);

        stripeService.updatePaymentMethod(sub.getStripeCustomerId(), paymentMethodId);
        log.info("Payment method updated for company {}", companyId);
    }

    @Override
    @Transactional
    public DeactivationStatusResponse deactivateAccount(UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND", "Company not found"));

        if (company.getDeactivationRequestedAt() != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "DEACTIVATION_ALREADY_REQUESTED",
                    "A deactivation request is already pending for this company");
        }

        Instant now = Instant.now();
        company.setDeactivationRequestedAt(now);
        company.setDeletionScheduledAt(now.plus(30, ChronoUnit.DAYS));
        company.setStatus(CompanyStatus.SUSPENDED);
        companyRepository.save(company);

        logAccountAction("ACCOUNT_DEACTIVATION_REQUESTED", companyId, Map.of(
                "deletionScheduledAt", company.getDeletionScheduledAt().toString()
        ));

        log.info("Deactivation requested for company {}, deletion scheduled at {}", companyId, company.getDeletionScheduledAt());
        return new DeactivationStatusResponse(true, company.getDeactivationRequestedAt(), company.getDeletionScheduledAt());
    }

    @Override
    @Transactional
    public DeactivationStatusResponse reactivateAccount(UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND", "Company not found"));

        if (company.getDeactivationRequestedAt() == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "NO_DEACTIVATION_REQUESTED",
                    "No pending deactivation request found for this company");
        }

        company.setDeactivationRequestedAt(null);
        company.setDeletionScheduledAt(null);
        company.setStatus(CompanyStatus.ACTIVE);
        companyRepository.save(company);

        logAccountAction("ACCOUNT_REACTIVATED", companyId, null);

        log.info("Deactivation cancelled for company {}", companyId);
        return new DeactivationStatusResponse(false, null, null);
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

    private void logAccountAction(String action, UUID companyId, Map<String, Object> metadata) {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            UUID actorUserId = null;
            UUID actorRoleAssignmentId = null;
            PlatformRole actorRole = null;
            if (auth != null && auth.getPrincipal() instanceof AuthSessionPrincipal p) {
                actorUserId = p.userId();
                actorRoleAssignmentId = p.roleAssignmentId();
                actorRole = p.role();
            }
            auditLogService.logAction(new AuditLog(
                    companyId, actorUserId, actorRoleAssignmentId, actorRole,
                    null, action, "COMPANY", companyId, null, metadata, null));
        } catch (Exception ignored) {
        }
    }
}
